GHCO Trading Application - PnL Positions

Build project using maven
Run GHCOTradingApplication on port 8080

Access docs for loading/aggregating via API here: http://localhost:8080/swagger-ui/index.html

Via Postman or response visualiser, run `POST http://localhost:8080/api/v1/trade/aggregateAndVisualise` with same
body object as is present for `POST http://localhost:8080/api/v1/trade/aggregate` found via swagger. Here you will
be able to see a graph representation of the PnL data