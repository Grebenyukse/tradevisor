insert into tradevisor.signals ("name",description,ticker_code,direction,
price_open,stop_loss,take_profit,created_at,updated_at,status,strategy_props) values
	 ('fibo',null,'TEST_TICKER@TEST_EXCHANGE',1,3.629,0.0,11.742,'2025-10-30 09:03:50.75795+03',
	 null,'CREATED','[{"color": "#FF0000", "label": "Fibo 38.2%  7.258", "style": "dashed",
	  "toUtc": 1761591830.757950000, "fromUtc": 1761804230.757950000, "toPrice": 7.258, "fromPrice": 7.258},
	   {"color": "blue", "label": "BUY ", "style": "solid", "toUtc": 1761591830.757950000,
	    "fromUtc": 1761804230.757950000, "toPrice": 3.629, "fromPrice": 3.629},
	     {"color": "green", "label": "TP fibo 61.8%: 11.742; 8.113 pts; 223.56%; 2.24 tp/sl ratio.",
	     "style": "solid", "toUtc": 1761591830.757950000, "fromUtc": 1761804230.757950000, "toPrice": 11.742,
	      "fromPrice": 11.742}, {"color": "red", "label": "SL  0.0; 3.629 pts; 100.0 % ", "style": "solid",
	       "toUtc": 1761591830.757950000, "fromUtc": 1761804230.757950000, "toPrice": 0.0, "fromPrice": 0.0},
	       {"color": "#FFFF00", "label": "", "style": "dashed", "toUtc": 1761591830.757950000, "fromUtc": 1761660230.757950000, "toPrice": 19.0, "fromPrice": 0.0}, {"color": "blue", "label": "Touch", "style": "round", "toUtc": 1761764630.757950000, "fromUtc": 1761764630.757950000, "toPrice": 7.258, "fromPrice": 7.258}, {"color": "blue", "label": "Touch", "style": "round", "toUtc": 1761692630.757950000, "fromUtc": 1761692630.757950000, "toPrice": 7.258, "fromPrice": 7.258}]');
