# gRPC Example

This is an example of authenticating with Betfair and then exposing the Betfair API via gRPC endpoints.

To run the example:

```shell
sbt example-grpc/run
```

Then, issue gRPC commands from your client of choice, for example, with `grpcurl`:

```shell
grpcurl -d '{}' -plaintext localhost:8080 com.thinkmorestupidless.betfair.proto.navigation.NavigationService/GetMenu
```

## Server Reflection

By default the server has server reflection enabled so you can use that to list and describe services:

```shell
➜ grpcurl -plaintext localhost:8080 list
com.thinkmorestupidless.betfair.proto.exchange.ExchangeService
com.thinkmorestupidless.betfair.proto.navigation.NavigationService
grpc.reflection.v1alpha.ServerReflection
➜ grpcurl -plaintext localhost:8080 describe com.thinkmorestupidless.betfair.proto.navigation.NavigationService
com.thinkmorestupidless.betfair.proto.navigation.NavigationService is a service:
service NavigationService {
  rpc GetMenu ( .com.thinkmorestupidless.betfair.proto.navigation.GetMenuRequest ) returns ( .com.thinkmorestupidless.betfair.proto.navigation.Menu );
}
```
