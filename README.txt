___GHCO Trading Application - PnL Positions___

Background:
Application to aggregate trade data based on CSV files. Initial sample set will be loaded on startup and a PnL position aggregation
on the whole data set “per BBGCode, per portfolio, per strategy, per user” will be actioned. To see the results set either
"aggregation.outputToCsv" or "aggregation.outputToConsole" in application.yml to true. Note, the results for this initial
aggregation is converted into USD via a simple FX service (can be split into currency as extra level of aggregation too,
but naturally doesn't make sense to aggregate together values from different currencies).

Went a step further to allow for aggregations of different criteria, with optional filters and optional currency conversion.

Further trades can be loaded in via a drop of a new CSV file into the "/resources/data/input" directory while the application
is running or before startup, or via the API. Further aggregations can also be executed via the API.
API docs/platform for executing trade loading/aggregating can be found here: http://localhost:8080/swagger-ui/index.html
Alternatively the calls described in the API can be actioned the same via "curl" or via Postman.

Via Postman or alternative REST response visualiser, run `POST http://localhost:8080/api/v1/trade/aggregateAndVisualise`
with same request body as is present for `POST http://localhost:8080/api/v1/trade/aggregate` found via swagger.
Here you will be able to see a graphical representation of the PnL data. You may need to add these lines to Postman->Tests
to be able to visualise the data:
****
const template = pm.response.text();
pm.visualizer.set(template);
****

Steps to run the application:
- Build project using maven via the pom.xml
- Change "file.baseDirectory" in application.yml to ensure input/output files are read/written correctly (should work with
relative path but using absolute to be sure
- Run com.tobycc.ghcoTrading.GHCOTradingApplication on port 8080

Some test cases can be found in "/resources/Test Cases". Feel free to have a play through them. Don't forget to clear
the "input" directory of every file except the "sample_trades.csv" before each test case.
