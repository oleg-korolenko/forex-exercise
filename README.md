# forex-exercise
Local proxy for Forex Forge API currency conversion


## Before running the app
please add the api key for Forge API in `application.conf` file

## Big lines 


BlazeHttpClient` is injected in corresponding services on the the app init
 
 
#### Http routes 
One basic  route is used `GET /rates?from={from}&to={to}`  

Errors from services are mapped to specific program ones with diff Http codes

##### Param validation
Added validation for parsed query params

#### Program
One program uses 2 services `RatesService` and `QuotaService`

Main flow is simple:
- we check if quota is not yet used up
- then we check for rates   

(!) there's '/market' API exposed by Forge so we could check in the beginning of the flow if it's open/closed but I guess it's not adding a lot to the exercises cause it will be basically the same check as for quota 

##### QuotaService
Calls Forge API :  `https://forex.1forge.com/1.0.3/quota?api_key={{api_key}}`

If quota is used up we return a specific error. 

##### RatesService
Calls Forge API :  ` https://forex.1forge.com/1.0.3/convert?from={{from}}&to=={{from}}&quantity=1&api_key={{api_key}}`
and returns conversion information. 



##### Forge API error management
In general Forge API is quite leaky and inconsistent.
In some cases proper HTTP error codes are used and in some just 200 with some error fields. 

In general in services I tried to treat both cases and took some shortcuts in matching all error codes in the same way. 
I guess that for the final user difference between a bad request (from a service call) or server error is not really relevant.

Errors are typed and mapped to specific Program errors

Requests and responses are logged.


#### Testing
Unit tests for :
- service live interpreters
- main program


#### Logging
Used standard `Logging` middleware for incoming HttpCalls as well for the ones we emit via `HttpClient` 
  
## TODO's and improvements 
- logging without correlation id is not that useful. investigate : https://gvolpe.github.io/http4s-tracer/ 

- better centralized  error management for routes  (HttpErrorHandler) 


