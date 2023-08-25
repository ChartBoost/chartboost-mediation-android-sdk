# Error Codes

During an ad's lifecycle, there may be situations in which the ad may fail. In such cases, the Helium SDK creates an error object that includes a Helium Error Code along with a message describing the error.

Code Name | Code Number | Description |
----------|-------------|-------------|
NO_AD_FOUND | 0 | The Helium SDK did not fill the ad.
NO_NETWORK | 1 | No Internet connectivity was detected.
NO_BID_RETURNED | 2 | No bid was received from the server.
NO_BID_PAYLOAD | 3 | Bid was received, but had no content.
BID_PAYLOAD_NOT_VALID | 4 | The received bid had invalid content.
OTHER_BID_FAILURE | 5 | Bid returned was invalid
SERVER_ERROR | 6 | The Helium SDK received an error from the server.
PARTNER_ERROR | 7 | The Helium SDK received an error from the partner adapter.
PARTNER_SDK_NOT_LINKED | 8 | The partner SDK is not integrated.
INVALID_CREDENTIALS | 9 | Invalid credentials were used.
INVALID_CONFIG | 10 | The configuration was invalid.
INTERNAL | 11 | An internal Helium SDK error occurred.
PARTNER_SDK_TIMEOUT | 12 | The Helium SDK timed out the partner SDK.
AD_TYPE_NOT_SUPPORTED | 13 | An unsupported ad type was used or found.
PARTNER_SDK_NOT_INITIALIZED | 14 | The partner SDK failed to initialize.
RATE_LIMITED | 15 | The load request has been rate limited.
