package ru.grnk.tradevisor.integration.finam.tradeclient.riskonly;

import com.google.type.Decimal;
import grpc.tradeapi.v1.assets.AssetsServiceGrpc;
import grpc.tradeapi.v1.auth.AuthRequest;
import grpc.tradeapi.v1.auth.AuthServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.grnk.tradevisor.calculate.signals.TrvSignalStatus;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;
import ru.grnk.tradevisor.common.properties.TrvFinamProperties;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.common.repository.entity.Signals;
import ru.grnk.tradevisor.integration.finam.BearerToken;
import ru.grnk.tradevisor.integration.finam.tradeclient.OpenPositionClient;
import ru.grnk.tradevisor.integration.rts.RtsService;
import ru.grnk.tradevisor.integration.rts.dto.ContractParams;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

import static java.util.Optional.ofNullable;
import static ru.grnk.tradevisor.common.util.RoundPriceUtils.moneyToBigDecimal;
import static ru.grnk.tradevisor.common.util.RoundPriceUtils.roundPrice;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.integration.finam.open-position-client", havingValue = "riskonly")
public class FinamRiskCountOnlyTradeClient implements OpenPositionClient {
    public static final BigDecimal AVERAGE_COMMISSION = BigDecimal.valueOf(0.002f);
    public static final BigDecimal GO_LEVEL = BigDecimal.valueOf(0.15f);
    private static final BigDecimal RISK_LEVEL = BigDecimal.valueOf(0.02);
    private final TradevisorProperties tradevisorProperties;
    private final RtsService rtsService;
    private final SignalsRepository signalsRepository;

    private final AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;
    private final AssetsServiceGrpc.AssetsServiceBlockingStub assetsServiceBlockingStub;

    @Override
    public boolean openPosition(String symbol, float priceOpen, float stopLoss, float takeProfit, int direction, int signalId) {
        var assetParams = getAssetParams(symbol);
        if (!assetParams.getTradeable()) {
            log.warn("symbol is not tradeable: {}", symbol);
            return false;
        }
        if (assetParams.getLongable().getValueValue() == 0) { //log запрещен. мы таким не торгуем
            log.warn("long operations restricted for symbol: {}", symbol);
            return false;
        }
        if (direction == -1 && assetParams.getShortable().getValue().getNumber() == 0) {
            log.warn("short operations restricted and signal for sell for symbol: {}", symbol);
            return false;
        }
        var asset = getAsset(symbol);
        var minPriceStep = BigDecimal.valueOf(asset.getMinStep())
                .divide(BigDecimal.TEN.pow(asset.getDecimals()),
                        asset.getDecimals(),
                        RoundingMode.UNNECESSARY);
        BigDecimal normalizedPriceOpen = roundPrice(priceOpen, minPriceStep, direction);
        BigDecimal normalizedStopLoss = roundPrice(stopLoss, minPriceStep, direction);
        BigDecimal normalizedTakeProfit = roundPrice(takeProfit, minPriceStep, direction);
        var tradingLot = getTradingLot(symbol, normalizedPriceOpen, normalizedStopLoss, normalizedTakeProfit, direction);
        if (tradingLot.intValue() == 0) {
            log.warn("not enough money to open position. signalId: {} , symbol:{}", signalId, symbol);
            return false;
        }
        signalsRepository.saveSignal(Signals.builder()
                .tickerCode(symbol)
                .takeProfit(normalizedTakeProfit.floatValue())
                .stopLoss(normalizedStopLoss.floatValue())
                .priceOpen(normalizedPriceOpen.floatValue())
                .name(null)
                .status(TrvSignalStatus.MANUAL.name())
                .description("manual trades signal for:" + signalId)
                .direction(Short.valueOf(String.valueOf(direction)))
                .updatedAt(OffsetDateTime.now())
                .riskLot(tradingLot.floatValue())
                .tpTicks(normalizedPriceOpen.subtract(normalizedTakeProfit).abs().floatValue())
                .slTicks(normalizedPriceOpen.subtract(normalizedStopLoss).abs().floatValue())
                .tp2SlRatio(normalizedPriceOpen.subtract(normalizedTakeProfit)
                        .divide(normalizedPriceOpen.subtract(normalizedStopLoss), RoundingMode.DOWN)
                        .abs().floatValue())
                .build());
        return true;
    }

    /**
     * trading_lot = round.down(counted_lot, min_lot)
     * min_lot = минимальный лот, который можно выставить по инструменту
     * counted_lot = Min(risk_lot, available_lot)
     * risk_lot = (balance * risk_leve ) / (((price_open - stop_loss) / tick_size) * tick_price)
     * tick_steps = (price_open - stop_loss) / minimal_price_step
     * tick_price - стоимость шага цены
     * - получить через rts_api если есть, если нет = 1 руб
     * available_lot = available_money / (go + ((price_open - sl) / tick_size) * tick_price + avg_commission_rate*go_price*lot_factor)
     * - go = direction= 1 ? longCollateral : shortCollateral. данные от брокера
     * - lot_factor = количество элементов актива в одном лоте
     * - avg_commission_rate = 0,05 (5% на объем сделки)
     * - available_money
     * available_money = balance + variance_margin - sum(open_risk + locked_money + commission)
     * - balance = accountRs.getCashList().stream().filter(x -> x.getCurrencyCode().equals("RUB")).findFirst().map(RoundPriceUtils::moneyToBigDecimal).orElseThrow();
     * - variance_margin = вариационная маржа портфеля с момента открытия позиции
     * - sum - сумма по всем теоретическим позициям
     * - open_risk = ((last_clearing_price - weighted_sl) / tick_size) * tick_price * quantity
     * - last_clearing_price - цена последнего клиринга
     * - weighted_sl - средневзвешенная цена STOP_LOSS
     * - quantity - размер сделки в лотах
     * - commission = (open_position_fee + close_position_fee + clearing_fee*position_hold + overnight_fee*average_position_hold)*quantity
     * - locked_money = quantity * go
     * - quantity - размер позиции в лотах
     * params:
     * BigDecimal priceOpen - нормализованная цена открытия (округлена до ближайшего тика с учетом tick_size)
     * BigDecimal stopLoss - нормализованная цена stop loss (округлена до ближайшего тика с учетом tick_size)
     * BigDecimal takeProfit - нормализованная цена take profit (округлена до ближайшего тика с учетом tick_size)
     * int direction - направление 1 - long, -1 short
     *
     * @return lot amount to trade
     */
    private BigDecimal getTradingLot(String symbol, BigDecimal priceOpen, BigDecimal stopLoss, BigDecimal takeProfit, int direction) {
        var balance = BigDecimal.valueOf(tradevisorProperties.trade().riskMoney().rus());
        var assetParams = getAssetParams(symbol);
        var asset = getAsset(symbol);
        var priceMinStep = BigDecimal.valueOf(asset.getMinStep())
                .divide(BigDecimal.TEN.pow(asset.getDecimals()),
                        asset.getDecimals(),
                        RoundingMode.UNNECESSARY);
        var lotSize = bigDecimalFromDecimal(asset.getLotSize());
        BigDecimal go = direction == 1 ? moneyToBigDecimal(assetParams.getLongCollateral()) : moneyToBigDecimal(assetParams.getShortCollateral());
        var ticker = symbol.split("@")[0];
        BigDecimal tickPrice = ofNullable(rtsService.getContractParams(ticker))
                .map(ContractParams::getFullTickValue)
                .orElse(BigDecimal.ONE);
        if (!isValidPriceConfiguration(priceOpen, stopLoss, takeProfit, direction)) {
            throw new IllegalStateException("invalid sl or tp ");
        }
        BigDecimal riskPerLot = calculateRiskPerLot(priceOpen, stopLoss, priceMinStep, tickPrice);
        if (riskPerLot.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // risk_lot = (balance * risk_level) / risk_per_lot
        BigDecimal riskLot = balance.multiply(RISK_LEVEL)
                .divide(riskPerLot, RoundingMode.DOWN);
        BigDecimal availableLot = calculateAvailableLot(go, riskPerLot, balance);
        BigDecimal countedLot = riskLot.min(availableLot);
        if (lotSize.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // Округляем до целых лотов
        BigDecimal tradingLot = countedLot.setScale(0, RoundingMode.DOWN);
        return tradingLot.max(BigDecimal.ZERO);
    }

    public BearerToken getBearer() {
        TrvFinamProperties finamProperties = tradevisorProperties.integration().finam();
        var authRs = authServiceBlockingStub.auth(AuthRequest.newBuilder()
                .setSecret(finamProperties.secret())
                .build());
        return new BearerToken(authRs.getToken());
    }

    private grpc.tradeapi.v1.assets.GetAssetResponse getAsset(String symbol) {
        return assetsServiceBlockingStub.withCallCredentials(getBearer())
                .getAsset(grpc.tradeapi.v1.assets.GetAssetRequest.newBuilder()
                        .setSymbol(symbol)
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
    }

    private grpc.tradeapi.v1.assets.GetAssetParamsResponse getAssetParams(String symbol) {
        return assetsServiceBlockingStub.withCallCredentials(getBearer())
                .getAssetParams(grpc.tradeapi.v1.assets.GetAssetParamsRequest.newBuilder()
                        .setSymbol(symbol)
                        .setAccountId(tradevisorProperties.integration().finam().accountId())
                        .build());
    }

    private boolean isValidPriceConfiguration(BigDecimal priceOpen, BigDecimal stopLoss, BigDecimal takeProfit, int direction) {
        if (direction > 0) {
            // LONG: SL должен быть ниже цены открытия, TP выше
            if (stopLoss.compareTo(priceOpen) >= 0) {
                log.warn("Invalid stop loss for LONG position. SL: {}, Open: {}", stopLoss, priceOpen);
                return false;
            }
            if (takeProfit.compareTo(priceOpen) <= 0) {
                log.warn("Invalid take profit for LONG position. TP: {}, Open: {}", takeProfit, priceOpen);
                return false;
            }
        } else {
            // SHORT: SL должен быть выше цены открытия, TP ниже
            if (stopLoss.compareTo(priceOpen) <= 0) {
                log.warn("Invalid stop loss for SHORT position. SL: {}, Open: {}", stopLoss, priceOpen);
                return false;
            }
            if (takeProfit.compareTo(priceOpen) >= 0) {
                log.warn("Invalid take profit for SHORT position. TP: {}, Open: {}", takeProfit, priceOpen);
                return false;
            }
        }
        return true;
    }

    private BigDecimal calculateRiskPerLot(BigDecimal priceOpen, BigDecimal stopLoss, BigDecimal minStep, BigDecimal tickPrice) {
        return priceOpen.subtract(stopLoss)
                .abs()
                .divide(minStep, RoundingMode.HALF_UP)
                .multiply(tickPrice);
    }

    private BigDecimal calculateAvailableLot(BigDecimal go, BigDecimal riskPerLot, BigDecimal availableMoney) {
        BigDecimal lockedMarginComponent = go.multiply(GO_LEVEL);
        BigDecimal slComponent = riskPerLot;
        BigDecimal commissionComponent = go.multiply(AVERAGE_COMMISSION);

        BigDecimal totalCostPerLot = lockedMarginComponent.add(slComponent).add(commissionComponent);

        if (totalCostPerLot.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return availableMoney.divide(totalCostPerLot, RoundingMode.DOWN);
    }

    private static BigDecimal bigDecimalFromDecimal(Decimal decimal) {
        return new BigDecimal(decimal.getValue());
    }


}
