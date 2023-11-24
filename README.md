# Less Stupid Betfair Library

![Release Status](https://github.com/thinkmorestupidless/less-stupid-betfair/actions/workflows/release.yml/badge.svg)
![Compile/Test Status](https://github.com/thinkmorestupidless/less-stupid-betfair/actions/workflows/pull-request.yml/badge.svg)

`less-stupid-betfair` is designed to make it easy as possible to interact with the Betfair API from Scala code.

## Getting Started

Getting started using the Betfair API is not quite as straightforward as you might be used to. Betfair uses Mutual TLS to encrypt communication between client and server so as well as username/password you'll need a self-signed certificate registered with your Betfair account. 

### Creating a Certificate

Betfair themselves provide pretty good documentation on this. If you don't want to bother reading their docs, however, you can just use the `tools/certs/gen-key-stores.sh` script. This will prompt you for some details and then produce a `client-2048.crt` file that you should upload to your Betfair account. It wil also produce a `client-2047.p12` file which you'll use to create a Java Key Store for encrypting the requests made to Betfair.

### Get your Credentials

You'll need three credentials for accessing the Betfair API:

- username
- password
- application key

The username and password are the same ones you'd use to log in to Betfair's website. The application key is available via an API call. Yes, that's right, in order to access the Betfair API you need to use the Betfair API...

Betfair provide a way around this - [The Accounts API Demo Tool](https://developer.betfair.com/en/exchange-api/accounts-api-demo/) - to use it:

- Visit the tool site
- Invoke the `getDeveloperAppKeys` call
- Grab the `delayed` key

> The `delayed` key is exactly what it's name suggests - it provides you the live data... just delayed by a couple of minutes. It's important to use the delayed key until you're ready to go into production.

## Setup the `.env`

Copy the `.env.template` and name the new file `.env`
Fill in the missing details using the information above:

```shell
BETFAIR_USERNAME = <Your Betfair username>
BETFAIR_PASSWORD = <Your Betfair password>
BETFAIR_APPLICATION_KEY = <The application key you retrieved above>
BETFAIR_CERT_FILE = <The absolute path to the `client-2048.crt` file you created above>
BETFAIR_CERT_PASSWORD = <The password you provided when creating the client certificate>
```

## Run the examples

To confirm you're able to correctly authenticate with the Betfair API run one (or more) of the examples. They'll pull their configuration from the `.env` file and if authentication fails you should receive some meaningful error messages to point you in the right direction (or, of course, you can raise a new issue).

To confirm you can authenticate and stream data:

```shell
sbt example-streams-api/run
```

