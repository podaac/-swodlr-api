# swodlr-api
A GraphQL API for SWODLR

SWODLR has been developed from the ground up as a GraphQL-first service giving
developers a more flexible way to interact with the system. This API is intended
for both direct frontend and backend consumption

## Authentication

SWODLR-API utilizes bearer tokens from
[Earthdata Login](https://urs.earthdata.nasa.gov/) as its primary authentication
mechanism. Once a bearer token is generated, simply include the bearer token as
part of a request's `Authorization` header.

```
Authorization: Bearer eyJ0e...
```

SWODLR-API currently does not provide support for username/passwords being
used in Basic authentication as some other Earthdata APIs may allow.

### Frontend Integration

SWODLR-API does not provide any mechanism to perform direct authentication via
a user's credentials (username/password); this is by design. SWODLR-API takes a
"client-first" approach to authentication leaving it up to our integrators to
provide authentication to their users as they see fit against Earthdata Login.
For example, on
[SWODLR-UI](https://github.com/podaac/swodlr-ui), we perform authentication
purely via the frontend with OAuth2 Primary Key Code Exchange which allows for
secure authentication to Earthdata Login without needing to publish our client
secret publicly and securing the authentication between the client and Earthdata
Login.

Unfortunately, Earthdata Login does not currently provide support for OAuth2's
PKCE extension. Instead SWODLR provides a OAuth2 PKCE wrapper which enables this
authentication flow by wrapping around the existing Earthdata Login OAuth2
endpoints. These PKCE-enabled endpoints are compliant with RFC 7636 and are
available at:

```
/api/edl/oauth/authorize
/api/edl/oauth/token
```

These endpoints are provided as a courtesy to frontend integrators while
Earthdata Login works on providing proper support for PKCE.

## Design

SWODLR-API does not directly provide raster generation/job tracking on its own.
Instead SWODLR-API acts as a caller to
[swodlr-raster-create](https://github.com/podaac/swodlr-raster-create) which
performs these actions asynchronously. SWODLR-API instead acts as the
authentication and caching layers for the rest of the SWODLR system as it is the
only publicly accessible portion of the system. All other microservices are not
directly user invocable or publicly accessible.

### Security

Once a user's request leaves SWODLR-API, SWODLR's microservices assume that
requests have been fully authenticated and are not required to perform further
authentication checks. Therefore it is important that SWODLR performs proper
authentication/authorization checks before any request leaves its scope.

SWODLR-API utilizes Earthdata Login as its primary authentication provider. As
noted above, SWODLR-API's authentication is handled by Earthdata Login bearer
tokens. These tokens are encoded as
[JSON Web Tokens](https://datatracker.ietf.org/doc/html/rfc7519) which allow
us as an API provider to verify these tokens without requiring a call to
Earthdata Login by verifying the cryptographic signature in the token against
the public key of Earthdata Login.

Additionally, SWODLR-API also provides functionality which is specifically gated
behind Earthdata Login user roles. These roles are only assignable by
application administrators as part of Earthdata Login's OAuth2 implementation.
SWODLR-API retrieves a user's roles in the course of its request-response flow
and authorizes certain endpoints based on those roles.

### Generation Workflow

SWODLR-API is ***NOT*** a job management system and it does ***NOT*** provide
users with direct access to the underlying Science Data System (SDS). Instead
SWODLR-API is designed around the idea that a customized product generated for
one user is the same product that would be generated for another user.

As such, when a user requests a product for generation, SWODLR-API will first
evaluate if such a product has already been generated in the past. If it has,
then that existing product will be returned and the SDS will not be invoked for
a new product generation. It is only if the product has not been generated in
the past that the SDS will be invoked to generate a new product. This approach
has obvious cost savings benefits for products that will be generated repeatedly
by multiple users. 

Once a request is submitted to SWODLR-API and the API's internal cache does not
contain an existing product, SWODLR-API invokes the swodlr-raster-create Step
Function by sending a message to its SNS topic. SWODLR-API will then mark that
product as `NEW` in its database. This is where SWODLR-API ends its interaction
with this request directly and where the rest of SWODLR's microservices take
over.

SWODLR was designed to be highly-scalable and asynchronous from day one. Each
microservice of SWODLR is designed to perform a specific task and report back
its results throughout execution. For example, `swodlr-raster-create` will
provide updates back to `swodlr-api` for the user to monitor a product's
generation process. It performs this update by interfacing with another
microservice, `swodlr-async-update`, which contains the logic to update
`swodlr-api`'s database to reflect any new status changes to products.

SWODLR-API does not directly perform monitoring during a microservice's
execution and leaves it up to the microservices to perform the reporting back to
the database as they execute. This approach has several advantages, but the main
being that SWODLR-API doesn't need to scale according to the number of products
that are being generated. Instead SWODLR-API is dedicated to just servicing API
requests and only needs to be scaled as such to handle the demand of API
requests.

Services such as `swodlr-raster-create` and `swodlr-async-update` are built on
top of AWS serverless technologies such as Step Functions and Lambdas, and can
automatically scale SWODLR during periods of high demand without any issue.

As mentioned before, SWODLR-API is ***NOT*** a job management system. Instead
SWODLR leans on existing SDS systems such as HySDS for these features.
SWODLR-API also does ***NOT*** interface directly with these SDS's directly;
instead performing SDS actions via its microservices. This design has the
advantage of narrowing scope for each of SWODLR's components and creating
security boundaries for the information that is passed between SWODLR's
components.
