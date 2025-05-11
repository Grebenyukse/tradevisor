package ru.grnk.tradevisor.common.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.SupParameters;

import java.util.Map;
import java.util.stream.Collectors;

import static ru.grnk.tradevisor.dbmodel.Tables.SUP_PARAMETERS;

@Repository
@RequiredArgsConstructor
public class ParametersRepository {

    private final DSLContext dslContext;

    public Map<String, String> getAllParameters() {
        return dslContext.select().from(SUP_PARAMETERS)
                .fetchStreamInto(SupParameters.class)
                .collect(Collectors.toMap(
                        SupParameters::getPropertyName,
                        SupParameters::getPropertyValue,
                        (t1, t2) -> t2));
    }

    public void setValue(String key, String value) {
        dslContext.update(SUP_PARAMETERS)
                .set(SUP_PARAMETERS.PROPERTY_VALUE, value)
                .where(SUP_PARAMETERS.PROPERTY_NAME.eq(key))
                .execute();
    }


}
