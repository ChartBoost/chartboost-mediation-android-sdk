package com.chartboost.sdk.internal.AdUnitManager.loaders

import android.view.ViewGroup
import com.chartboost.sdk.api.WebView.WebViewGetResponseBuilder
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnitBannerData
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.parsers.AdUnitParser
import com.chartboost.sdk.internal.AdUnitManager.parsers.OpenRTBAdUnitParser
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.ConfigurationBodyFields
import com.chartboost.sdk.internal.Model.DeviceBodyFields
import com.chartboost.sdk.internal.Model.IdentityBodyFields
import com.chartboost.sdk.internal.Model.PrivacyBodyFields
import com.chartboost.sdk.internal.Model.ReachabilityBodyFields
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Model.RequestBodyFields
import com.chartboost.sdk.internal.Model.SessionBodyFields
import com.chartboost.sdk.internal.Networking.CBNetworkRequest
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.EndpointRepository
import com.chartboost.sdk.internal.Networking.defaultUrl
import com.chartboost.sdk.internal.Networking.requests.CBRequest
import com.chartboost.sdk.internal.Networking.requests.CBWebViewRequest
import com.chartboost.sdk.internal.Networking.requests.NetworkType
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.identity.TrackingState
import com.chartboost.sdk.internal.measurement.OpenMeasurementManager
import com.chartboost.sdk.internal.utils.Base64Wrapper
import com.chartboost.sdk.test.AssetDescriptor
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.iab.omid.library.chartboost.adsession.Partner
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdLoaderImplTest {
    private val interstitialTraits = AdType.Interstitial
    private val rewardedTraits = AdType.Rewarded
    private val bannerTraits = AdType.Banner
    private val base64WrapperMock = mockk<Base64Wrapper>()
    private val fileCacheMock = mockk<FileCache>()
    private val requestBodyBuilderMock = mockk<RequestBodyBuilder>()
    private val requestBodyFieldsMock = mockk<RequestBodyFields>()
    private val networkServiceMock = mockk<CBNetworkService>(relaxed = true)
    private val sessionBodyFieldsMock = mockk<SessionBodyFields>()
    private val configurationBodyFieldsMock = mockk<ConfigurationBodyFields>()
    private val deviceBodyFieldsMock = mockk<DeviceBodyFields>()
    private val identityBodyFieldsMock = mockk<IdentityBodyFields>()
    private val adUnitParserMock = mockk<AdUnitParser>()
    private val openRTBAdUnitParser = mockk<OpenRTBAdUnitParser>()
    private val openMeasurementManagerMock = mockk<OpenMeasurementManager>()
    private val reachabilityBodyFieldsMock = mockk<ReachabilityBodyFields>()
    private val privacyBodyFieldsMock = PrivacyBodyFields(1, mutableListOf(), 1, 1, null, null)
    private val bidResponse =
        "{\"id\": \"cac2faea23eeecc135bf4e678116c0c339fa915f\", \"seatbid\": [{\"bid\": [{\"id\": \"d46baee9d6b7c20f2f8fcca5d9a96244329fbbe3-1\", \"impid\": \"cac2faea23eeecc135bf4e678116c0c339fa915f\", \"price\": 0.07, \"adm\": \"PGhlYWQ+CjwvaGVhZD4KPGJvZHk+CiAgICA8aW1nIHNyYz0iaHR0cHM6Ly9jaGFydGJvb3N0LmFhcmtpLm5ldC9ydGIvcGl4ZWw/YXVjdGlvbl9pZD1kNDZiYWVlOWQ2YjdjMjBmMmY4ZmNjYTVkOWE5NjI0NDMyOWZiYmUzJmF1Y3Rpb25fY3VycmVuY3k9VVNEIiB3aWR0aD0iMSIgaGVpZ2h0PSIxIi8+PGltZyBzcmM9Imh0dHBzOi8vaW1wLmNvbnRyb2wua29jaGF2YS5jb20vdHJhY2svaW1wcmVzc2lvbj9jYW1wYWlnbl9pZD1rb3NvbGl0YWlyZS1hbmRyb2lkNTRjOTgwNmY4MTYyNGI2MTYwM2ZiYTM0ZDQmbmV0d29ya19pZD0zNCZzaXRlX2lkPTVhNGU3OTc1MzhhNWYwMGNmNjA3MzhkNiZwbGF0Zm9ybT1hbmRyb2lkJmRldmljZV9pZF90eXBlPWFkaWQmZGV2aWNlX2lkPTgyOGExMzQyLTdjMTItNDAxMS1hMzMwLTQ5ZTc2NmU1ZmIxZCZhYXJraV9pbXBfaWQ9Y2hhcnRib29zdCUzQWQ0NmJhZWU5ZDZiN2MyMGYyZjhmY2NhNWQ5YTk2MjQ0MzI5ZmJiZTMmYXBwX2lkPTQwMTFFMDUyQjFEOTlBQTlEQSIgd2lkdGg9IjEiIGhlaWdodD0iMSIvPgogICAgPHNjcmlwdCB0eXBlPSJ0ZXh0L2phdmFzY3JpcHQiIGlkPSJhYXJraV82YjVjOGJkODhmY2Y1OTUxZGEzY2Q3YmYwNjkxNzY5OCI+CihmdW5jdGlvbigpIHsKdmFyIHRhZyA9IGRvY3VtZW50LmNyZWF0ZUVsZW1lbnQoJ3NjcmlwdCcpLAogICAgb3V0U2NyaXB0ID0gZG9jdW1lbnQuZ2V0RWxlbWVudEJ5SWQoJ2FhcmtpXzZiNWM4YmQ4OGZjZjU5NTFkYTNjZDdiZjA2OTE3Njk4Jyk7Cgp0YWcuaWQgPSAnbW9ic3BpcmUtYWQtdGFnJzsKdGFnLnNyYyA9ICdodHRwczovL3JtLmFhcmtpLm5ldC92MS9hZHMvNmI1YzhiZDg4ZmNmNTk1MWRhM2NkN2JmMDY5MTc2OTgvYWQuanM/b3RzPScgKyAobmV3IERhdGUoKSkuZ2V0VGltZSgpKycmc2Vzc2lvbklkPWQ0NmJhZWU5ZDZiN2MyMGYyZjhmY2NhNWQ5YTk2MjQ0MzI5ZmJiZTMmbmV4dD1odHRwcyUzQSUyRiUyRmNoYXJ0Ym9vc3QuYWFya2kubmV0JTJGcnRiJTJGY2xpY2slM0ZhdWN0aW9uX2lkJTNEZDQ2YmFlZTlkNmI3YzIwZjJmOGZjY2E1ZDlhOTYyNDQzMjlmYmJlMyUyNmF1Y3Rpb25fY3VycmVuY3klM0RVU0QlMjZvZnIlM0Q4MzIyNSUyNmFkdmVydGlzaW5nX2lkJTNEODI4YTEzNDItN2MxMi00MDExLWEzMzAtNDllNzY2ZTVmYjFkJTI2ZmxpZ2h0JTNEJTdCYXJnX2ZsaWdodCU3RCUyNmNyZWF0aXZlJTNEJTdCYXJnX3RyYWNraW5nS2V5JTdEJTI2Y3YlM0QlN0JhcmdfY3YlN0QlMjZmbGlnaHRfcnVsZSUzRCU3QmFyZ19mbGlnaHRfcnVsZV9pZHMlN0Qmc2VjdXJlPXkmYWR2ZXJ0aXNpbmdfaWQ9ODI4YTEzNDI3YzEyNDAxMWEzMzA0OWU3NjZlNWZiMWQmcHJlX2xhbmc9RW5nbGlzaCZjaGFubmVsPWRzcF84MzIyNSZqc3RhZz15JmNiZD01JzsKdGFnLnR5cGUgPSAndGV4dC9qYXZhc2NyaXB0JzsKCm91dFNjcmlwdC5wYXJlbnROb2RlLmluc2VydEJlZm9yZSh0YWcsIG91dFNjcmlwdCk7Cn0pKCk7Cjwvc2NyaXB0Pgo8L2JvZHk+\", \"adomain\": [\"mobilityware.com/solitaire.php\"], \"bundle\": \"com.mobilityware.solitaire\", \"iurl\": \"https://spire.aarki.net/v1/ads/6b5c8bd88fcf5951da3cd7bf06917698/v1-23d6fbb63af625de95456601c7cfd868/screenshot.jpeg\", \"cid\": \"83225\", \"crid\": \"83225:23d6fbb63af625de95456601c7cfd868\", \"cat\": [\"IAB9-30\"], \"w\": 320, \"h\": 480, \"ext\": {\"prebid\": {\"targeting\": {\"hb_bidder\": \"chartboost\", \"hb_bidder_chartboost\": \"chartboost\", \"hb_creative_loadtype\": \"html\", \"hb_env\": \"mobile-app\", \"hb_env_chartboost\": \"mobile-app\", \"hb_pb\": \"0.00\", \"hb_pb_chartboost\": \"0.00\", \"hb_size\": \"320x480\", \"hb_size_chartboost\": \"320x480\"}, \"type\": \"video\"}, \"bidder\": {\"imptrackers\": [\"https://chartboost.aarki.net/rtb/win?auction_id=d46baee9d6b7c20f2f8fcca5d9a96244329fbbe3&auction_price=0.115&auction_currency=USD\"], \"crtype\": \"HTML5\", \"priceType\": 2, \"adId\": \"VyUdEMeZCkSbtF2ufD9uCJTXMrWhl6D21iEv36eOr11oguCi7869RZPe+cOdXyOvrpZQVTVZdfbZajXatSLAh0ghvQxM+O1SAZCzyOI1qxPZuCn7oEkMtPKzYjwI1zrYAeug68vw3lgnwW5HB6Pl27+cx8er82AQo4VuKYZEkofuJNHNfyxOE7vpckOJrEd6+l6LRb1vDGHFfsocO+d6b7Msft9j9Obcj6hYNwpJG0Rwx9LgdSmohaUotUCIpYgtWo1ufLJZSpg06nAOE0KxrYUDnKdV6bhM4RYl+Q6QspEdHEr3+ZH0GynX0ebXlXrtwjyuwnhdW2Q5A42Ky6WwogSYWWJ526ZKbK+3WyCffDPO6ZVAhgK+5Rzr4cJ3XdJZLXPlJTAKpkm4h6AWQQ86fxK5h4+hrfotu7tFQ//bJrFPPzRoe9GXr9AIt+nGGP5Kuc5+peih5a+107sqaDCEl2xMIJVif3iKYnkdS8Q8/FuvI/sriHwPtTdrkz1Mqx8FirmWjwM8RFhS155dEzKWFKYIHyioYvd92rnN1oL6LOokBIc+x8WAjlWD75ex+4dsvqkz8NckkoFEELL8F7rgSas++x1t6MufbkrHsUSHXX/8ndPKbVq96gvZ+8NF0ZJRl79Qr8UZ0+Y3WB+mXmqAAkkcXno5YlmYpuG95gIAsgea79CKmj0g8qKdhVFh1y9eJjxiYyqr5dY2iC5VegoO7yLjEgIkBf4o9a8TjZHBCkVO5fo7p3rAstrAmJ/ZGKOwcZiBHQ6fOmlXzq2UKfdA8Rp6yGVukgNuI9ZG4P2UDucM0jAJXnHsSBW1mcJy4X4j9SThH0r49ra9psI0cq0Cnl+2a2zEfuVHqe5Ga6mUkbG32XI2OqcuwqHfDuP86TKV8QJ136J8YPJtA1oT7IHA9O9R1JRGb0X1DI4EC7VVbhD52mPTAfQCbipj/KyL3XLfDyniT3p3ahNLvpOBssT98KNAR6ptLtGIxCY3bgkIXTOA8MOzEBAzvEeYu84h2W0ImdWz7tci9RiqieUvhtb3JlgW0LaLzsn417KBt7OC7qGGMpA/OrH3cn5ahNlNGHddGtxgP8uUFJEC5TNnNwTxFweMfUbOPMT62EQrOzvbhBUs48v3U/e4tEhAxVKtT9rdEU3GJdo0IgjP1b3Rh3dYvAuOZIzeOTAv5AlDN6HPJjMdbj4XIcO8/J50yvjza+LGexO8uzyGJD9aaagnNGzj2s3GrCMLdBhHrm2WUy+z2LfzEKaPvCcNENUzm+PHGoMVJWQJawyXMI+xJJCu8WJ8h/Q4PgWRtD0PltBvyDCkFMPTaDRJzef7562Pwtgut6rZPcMfVpEC819Cg5zUBH52+ESKj2nkfuvuKfM1owfSoQmBEPZZnBho5yOgABNLMdojuRYnxEfIkJk6yDp7JwGt4egNbcZTunyuxwyQFeq0gQ/OzFdF7/ORNXpksqi+zoRa5AbBMwMib7ieTLU7tZL5IjTUqQSDsSg7wnoiHBqYJ57pP+/BQabMR1wRPJI4hIKNtaJyx7ig8LcCffALDWCFaBU1yLAd0NWapJODYa7/pPot6tO3LDy6Q7xs7JX40rKDipemuLYDHdOS87S06+zsFLo1ZIy//J0U9xboIIf2ZBKGWE2Y1ZUXwsgUcBt40QDS8NbqLtORm54GqLwDRjcdJ6lm7qkc+PqVwSzgfUq6u2H767K1ixjM9SK4h494oA+ZzaHa5WVFKSSUoudNrkd9zPM3QRAbtPdREdwuVz4czOwXdxfYZfgTLXfRgpV+gtpK3GqtTABP592JDGsXGCjcE03ZnOgnVJPUb8BcLmnguHbaGbbazPZhnIfl3dr/cNAIWrgsr9ICJLJsYVY/n4n4cTD5hxrvQW2d8S/11yaRIPvMsah2yKrLZwfLGAhKAgKjmKT0wRtAg7NQ4iKMqMA50MZqhFRhRiXuXA6rsnp58dw8bUA+KEPQ+lPVk15DfCoY2JWw0Vai2sKFx2/S+VO6Njfz3EG70kWQYwvQFp6dK1KSL38=\", \"cgn\": \"HH9rcO14P26p1Hq3x7vyLrpLzjGnD9lNJnjTbL4M3xlqM9zrWxnLv5xSnYGpAUsJZjaL/AZPgQUgPRaNTtt8MITM9U5/VxHE+k+9EzUlKZKMOs91+jReUucwyBXph1TyhrwMSbQefSBoW7ARLPTLEA9m3OhxKb4huQ3dXjuua58scSNoGiKR4NvYJwXJdoveC6GN7rJv3sZGsu6kpQ1JswtBR8AiW/9xCbM3uB6teVQOb3YRy5xQzYBFyAKDv97J/qvc2g1w/W1EYOsG1Vp8r3bGltkiVJt/K4OC5zyhhkYNLpyHEonXsYeZSs52IE6uC9tMNeNhx2crxT2+kOymge0UUZ7njqqBX+UgBcDwo2Cftxa8l/hEtKmIN9bP6URhMZ5fq/50Gfaq9WKo+erC+l9Zg0CNyWRDtHmnaOyHCvMhqBmPHjJiKxBA32nusK2xvc7cJvDsKPq+hd0fcwUFD2rjrL7ZmxUVSGCUjterxOw5oDct1RKiyxOgfXa3dN5sW1RcH0D+R/JIxtH841W5Mv8/5aC7yK9zMqTGLQfG9e5jvR3hIbLHitWmsvJpBpZfTkA4MOzB/f2uzBI89MiovI6qLAc+cDOO3mYrWdP9DBeEchCBddMo6GFkcXfEQ1hMkEyDK1A6zSbb2SmIVt4fQM5MtDfQJQFZlPXcy+ms6XSdhJDkUFkZ3t9jpLXiabw0M7BkceZGk6l854TPI780WjJIIA==\", \"template\": \"https://t.chartboost.com/base_templates/html/mraid-iframe-open-df82555530.html\", \"videoUrl\": \"https://t.chartboost.com/cbvideo/test.mp4\", \"trackingId\": \"23a79da8-c861-418c-b67c-8d44a108a069\"}}}], \"seat\": \"chartboost\"}], \"ext\": {\"debug\": {\"httpcalls\": {\"chartboost\": [{\"uri\": \"http://internal-demandaggregator-production-elb-1948891915.us-east-1.elb.amazonaws.com:8080/auction/helium\", \"requestbody\": \"{\\\"regs\\\":{\\\"coppa\\\":0,\\\"ext\\\":{\\\"gdpr\\\":0}},\\\"id\\\":\\\"cac2faea23eeecc135bf4e678116c0c339fa915f\\\",\\\"imp\\\":[{\\\"id\\\":\\\"cac2faea23eeecc135bf4e678116c0c339fa915f\\\",\\\"banner\\\":{\\\"w\\\":1080,\\\"h\\\":2076,\\\"btype\\\":[4],\\\"battr\\\":[1,2],\\\"pos\\\":7,\\\"topframe\\\":1,\\\"ext\\\":{\\\"placementtype\\\":\\\"interstitial\\\",\\\"playableonly\\\":false,\\\"allowscustomclosebutton\\\":true}},\\\"video\\\":{\\\"mimes\\\":[\\\"video\\\\\\\\/mp4\\\"],\\\"minduration\\\":5,\\\"maxduration\\\":30,\\\"protocols\\\":[1,2,3],\\\"w\\\":1080,\\\"h\\\":2076,\\\"placement\\\":5,\\\"linearity\\\":1,\\\"skip\\\":1,\\\"battr\\\":[1,2],\\\"delivery\\\":[1,2],\\\"pos\\\":7,\\\"companiontype\\\":[1,3],\\\"ext\\\":{\\\"placementtype\\\":\\\"interstitial\\\"}},\\\"displaymanager\\\":\\\"Chartboost-Android-SDK\\\",\\\"displaymanagerver\\\":\\\"7.3.0\\\",\\\"instl\\\":1,\\\"tagid\\\":\\\"CBInterstitial\\\",\\\"bidfloor\\\":0.1,\\\"secure\\\":1,\\\"ext\\\":{\\\"viewabilityExtensions\\\":null}}],\\\"app\\\":{\\\"id\\\":\\\"5a4e797538a5f00cf60738d6\\\",\\\"name\\\":\\\"Super Mario Run\\\",\\\"bundle\\\":\\\"com.chartboost.heliumsdk.sampleapp\\\",\\\"storeurl\\\":\\\"market://details?id=com.nintendo.zara\\\",\\\"publisher\\\":{\\\"id\\\":\\\"596e66c543150f7e014a6660\\\",\\\"name\\\":\\\"Nintendo Co., Ltd.\\\"}},\\\"device\\\":{\\\"ua\\\":\\\"Chartboost-Android-SDK  7.3.0\\\",\\\"geo\\\":{\\\"lat\\\":40.5005,\\\"lon\\\":-3.66739,\\\"type\\\":2,\\\"country\\\":\\\"ES\\\",\\\"region\\\":\\\"Madrid\\\",\\\"city\\\":\\\"Madrid\\\",\\\"zip\\\":\\\"28001\\\"},\\\"lmt\\\":1,\\\"ip\\\":\\\"88.6.2.104\\\",\\\"devicetype\\\":4,\\\"make\\\":\\\"samsung\\\",\\\"model\\\":\\\"SM-G950F\\\",\\\"os\\\":\\\"Android\\\",\\\"osv\\\":\\\"8.0.0\\\",\\\"h\\\":2076,\\\"w\\\":1080,\\\"pxratio\\\":480,\\\"language\\\":\\\"English\\\",\\\"connectiontype\\\":2,\\\"ifa\\\":\\\"828a1342-7c12-4011-a330-49e766e5fb1d\\\"},\\\"user\\\":{\\\"geo\\\":{\\\"lat\\\":40.5005,\\\"lon\\\":-3.66739,\\\"type\\\":2,\\\"country\\\":\\\"ES\\\",\\\"region\\\":\\\"Madrid\\\",\\\"city\\\":\\\"Madrid\\\",\\\"zip\\\":\\\"28001\\\"}},\\\"test\\\":1,\\\"at\\\":1,\\\"tmax\\\":750,\\\"cur\\\":[\\\"USD\\\"],\\\"ext\\\":{\\\"prebid\\\":{\\\"targeting\\\":{}}}}\", \"responsebody\": \"{\\\"id\\\":\\\"cac2faea23eeecc135bf4e678116c0c339fa915f\\\",\\\"seatbid\\\":[{\\\"bid\\\":[{\\\"id\\\":\\\"d46baee9d6b7c20f2f8fcca5d9a96244329fbbe3-1\\\",\\\"impid\\\":\\\"cac2faea23eeecc135bf4e678116c0c339fa915f\\\",\\\"price\\\":0.07,\\\"adm\\\":\\\"PGhlYWQ+CjwvaGVhZD4KPGJvZHk+CiAgICA8aW1nIHNyYz0iaHR0cHM6Ly9jaGFydGJvb3N0LmFhcmtpLm5ldC9ydGIvcGl4ZWw/YXVjdGlvbl9pZD1kNDZiYWVlOWQ2YjdjMjBmMmY4ZmNjYTVkOWE5NjI0NDMyOWZiYmUzJmF1Y3Rpb25fY3VycmVuY3k9VVNEIiB3aWR0aD0iMSIgaGVpZ2h0PSIxIi8+PGltZyBzcmM9Imh0dHBzOi8vaW1wLmNvbnRyb2wua29jaGF2YS5jb20vdHJhY2svaW1wcmVzc2lvbj9jYW1wYWlnbl9pZD1rb3NvbGl0YWlyZS1hbmRyb2lkNTRjOTgwNmY4MTYyNGI2MTYwM2ZiYTM0ZDQmbmV0d29ya19pZD0zNCZzaXRlX2lkPTVhNGU3OTc1MzhhNWYwMGNmNjA3MzhkNiZwbGF0Zm9ybT1hbmRyb2lkJmRldmljZV9pZF90eXBlPWFkaWQmZGV2aWNlX2lkPTgyOGExMzQyLTdjMTItNDAxMS1hMzMwLTQ5ZTc2NmU1ZmIxZCZhYXJraV9pbXBfaWQ9Y2hhcnRib29zdCUzQWQ0NmJhZWU5ZDZiN2MyMGYyZjhmY2NhNWQ5YTk2MjQ0MzI5ZmJiZTMmYXBwX2lkPTQwMTFFMDUyQjFEOTlBQTlEQSIgd2lkdGg9IjEiIGhlaWdodD0iMSIvPgogICAgPHNjcmlwdCB0eXBlPSJ0ZXh0L2phdmFzY3JpcHQiIGlkPSJhYXJraV82YjVjOGJkODhmY2Y1OTUxZGEzY2Q3YmYwNjkxNzY5OCI+CihmdW5jdGlvbigpIHsKdmFyIHRhZyA9IGRvY3VtZW50LmNyZWF0ZUVsZW1lbnQoJ3NjcmlwdCcpLAogICAgb3V0U2NyaXB0ID0gZG9jdW1lbnQuZ2V0RWxlbWVudEJ5SWQoJ2FhcmtpXzZiNWM4YmQ4OGZjZjU5NTFkYTNjZDdiZjA2OTE3Njk4Jyk7Cgp0YWcuaWQgPSAnbW9ic3BpcmUtYWQtdGFnJzsKdGFnLnNyYyA9ICdodHRwczovL3JtLmFhcmtpLm5ldC92MS9hZHMvNmI1YzhiZDg4ZmNmNTk1MWRhM2NkN2JmMDY5MTc2OTgvYWQuanM/b3RzPScgKyAobmV3IERhdGUoKSkuZ2V0VGltZSgpKycmc2Vzc2lvbklkPWQ0NmJhZWU5ZDZiN2MyMGYyZjhmY2NhNWQ5YTk2MjQ0MzI5ZmJiZTMmbmV4dD1odHRwcyUzQSUyRiUyRmNoYXJ0Ym9vc3QuYWFya2kubmV0JTJGcnRiJTJGY2xpY2slM0ZhdWN0aW9uX2lkJTNEZDQ2YmFlZTlkNmI3YzIwZjJmOGZjY2E1ZDlhOTYyNDQzMjlmYmJlMyUyNmF1Y3Rpb25fY3VycmVuY3klM0RVU0QlMjZvZnIlM0Q4MzIyNSUyNmFkdmVydGlzaW5nX2lkJTNEODI4YTEzNDItN2MxMi00MDExLWEzMzAtNDllNzY2ZTVmYjFkJTI2ZmxpZ2h0JTNEJTdCYXJnX2ZsaWdodCU3RCUyNmNyZWF0aXZlJTNEJTdCYXJnX3RyYWNraW5nS2V5JTdEJTI2Y3YlM0QlN0JhcmdfY3YlN0QlMjZmbGlnaHRfcnVsZSUzRCU3QmFyZ19mbGlnaHRfcnVsZV9pZHMlN0Qmc2VjdXJlPXkmYWR2ZXJ0aXNpbmdfaWQ9ODI4YTEzNDI3YzEyNDAxMWEzMzA0OWU3NjZlNWZiMWQmcHJlX2xhbmc9RW5nbGlzaCZjaGFubmVsPWRzcF84MzIyNSZqc3RhZz15JmNiZD01JzsKdGFnLnR5cGUgPSAndGV4dC9qYXZhc2NyaXB0JzsKCm91dFNjcmlwdC5wYXJlbnROb2RlLmluc2VydEJlZm9yZSh0YWcsIG91dFNjcmlwdCk7Cn0pKCk7Cjwvc2NyaXB0Pgo8L2JvZHk+\\\",\\\"adomain\\\":[\\\"mobilityware.com/solitaire.php\\\"],\\\"bundle\\\":\\\"com.mobilityware.solitaire\\\",\\\"iurl\\\":\\\"https://spire.aarki.net/v1/ads/6b5c8bd88fcf5951da3cd7bf06917698/v1-23d6fbb63af625de95456601c7cfd868/screenshot.jpeg\\\",\\\"cid\\\":\\\"83225\\\",\\\"crid\\\":\\\"83225:23d6fbb63af625de95456601c7cfd868\\\",\\\"cat\\\":[\\\"IAB9-30\\\"],\\\"attr\\\":[],\\\"h\\\":480,\\\"w\\\":320,\\\"ext\\\":{\\\"imptrackers\\\":[\\\"https://chartboost.aarki.net/rtb/win?auction_id=d46baee9d6b7c20f2f8fcca5d9a96244329fbbe3&auction_price=0.115&auction_currency=USD\\\"],\\\"crtype\\\":\\\"HTML5\\\",\\\"priceType\\\":2,\\\"adId\\\":\\\"VyUdEMeZCkSbtF2ufD9uCJTXMrWhl6D21iEv36eOr11oguCi7869RZPe+cOdXyOvrpZQVTVZdfbZajXatSLAh0ghvQxM+O1SAZCzyOI1qxPZuCn7oEkMtPKzYjwI1zrYAeug68vw3lgnwW5HB6Pl27+cx8er82AQo4VuKYZEkofuJNHNfyxOE7vpckOJrEd6+l6LRb1vDGHFfsocO+d6b7Msft9j9Obcj6hYNwpJG0Rwx9LgdSmohaUotUCIpYgtWo1ufLJZSpg06nAOE0KxrYUDnKdV6bhM4RYl+Q6QspEdHEr3+ZH0GynX0ebXlXrtwjyuwnhdW2Q5A42Ky6WwogSYWWJ526ZKbK+3WyCffDPO6ZVAhgK+5Rzr4cJ3XdJZLXPlJTAKpkm4h6AWQQ86fxK5h4+hrfotu7tFQ//bJrFPPzRoe9GXr9AIt+nGGP5Kuc5+peih5a+107sqaDCEl2xMIJVif3iKYnkdS8Q8/FuvI/sriHwPtTdrkz1Mqx8FirmWjwM8RFhS155dEzKWFKYIHyioYvd92rnN1oL6LOokBIc+x8WAjlWD75ex+4dsvqkz8NckkoFEELL8F7rgSas++x1t6MufbkrHsUSHXX/8ndPKbVq96gvZ+8NF0ZJRl79Qr8UZ0+Y3WB+mXmqAAkkcXno5YlmYpuG95gIAsgea79CKmj0g8qKdhVFh1y9eJjxiYyqr5dY2iC5VegoO7yLjEgIkBf4o9a8TjZHBCkVO5fo7p3rAstrAmJ/ZGKOwcZiBHQ6fOmlXzq2UKfdA8Rp6yGVukgNuI9ZG4P2UDucM0jAJXnHsSBW1mcJy4X4j9SThH0r49ra9psI0cq0Cnl+2a2zEfuVHqe5Ga6mUkbG32XI2OqcuwqHfDuP86TKV8QJ136J8YPJtA1oT7IHA9O9R1JRGb0X1DI4EC7VVbhD52mPTAfQCbipj/KyL3XLfDyniT3p3ahNLvpOBssT98KNAR6ptLtGIxCY3bgkIXTOA8MOzEBAzvEeYu84h2W0ImdWz7tci9RiqieUvhtb3JlgW0LaLzsn417KBt7OC7qGGMpA/OrH3cn5ahNlNGHddGtxgP8uUFJEC5TNnNwTxFweMfUbOPMT62EQrOzvbhBUs48v3U/e4tEhAxVKtT9rdEU3GJdo0IgjP1b3Rh3dYvAuOZIzeOTAv5AlDN6HPJjMdbj4XIcO8/J50yvjza+LGexO8uzyGJD9aaagnNGzj2s3GrCMLdBhHrm2WUy+z2LfzEKaPvCcNENUzm+PHGoMVJWQJawyXMI+xJJCu8WJ8h/Q4PgWRtD0PltBvyDCkFMPTaDRJzef7562Pwtgut6rZPcMfVpEC819Cg5zUBH52+ESKj2nkfuvuKfM1owfSoQmBEPZZnBho5yOgABNLMdojuRYnxEfIkJk6yDp7JwGt4egNbcZTunyuxwyQFeq0gQ/OzFdF7/ORNXpksqi+zoRa5AbBMwMib7ieTLU7tZL5IjTUqQSDsSg7wnoiHBqYJ57pP+/BQabMR1wRPJI4hIKNtaJyx7ig8LcCffALDWCFaBU1yLAd0NWapJODYa7/pPot6tO3LDy6Q7xs7JX40rKDipemuLYDHdOS87S06+zsFLo1ZIy//J0U9xboIIf2ZBKGWE2Y1ZUXwsgUcBt40QDS8NbqLtORm54GqLwDRjcdJ6lm7qkc+PqVwSzgfUq6u2H767K1ixjM9SK4h494oA+ZzaHa5WVFKSSUoudNrkd9zPM3QRAbtPdREdwuVz4czOwXdxfYZfgTLXfRgpV+gtpK3GqtTABP592JDGsXGCjcE03ZnOgnVJPUb8BcLmnguHbaGbbazPZhnIfl3dr/cNAIWrgsr9ICJLJsYVY/n4n4cTD5hxrvQW2d8S/11yaRIPvMsah2yKrLZwfLGAhKAgKjmKT0wRtAg7NQ4iKMqMA50MZqhFRhRiXuXA6rsnp58dw8bUA+KEPQ+lPVk15DfCoY2JWw0Vai2sKFx2/S+VO6Njfz3EG70kWQYwvQFp6dK1KSL38=\\\",\\\"cgn\\\":\\\"HH9rcO14P26p1Hq3x7vyLrpLzjGnD9lNJnjTbL4M3xlqM9zrWxnLv5xSnYGpAUsJZjaL/AZPgQUgPRaNTtt8MITM9U5/VxHE+k+9EzUlKZKMOs91+jReUucwyBXph1TyhrwMSbQefSBoW7ARLPTLEA9m3OhxKb4huQ3dXjuua58scSNoGiKR4NvYJwXJdoveC6GN7rJv3sZGsu6kpQ1JswtBR8AiW/9xCbM3uB6teVQOb3YRy5xQzYBFyAKDv97J/qvc2g1w/W1EYOsG1Vp8r3bGltkiVJt/K4OC5zyhhkYNLpyHEonXsYeZSs52IE6uC9tMNeNhx2crxT2+kOymge0UUZ7njqqBX+UgBcDwo2Cftxa8l/hEtKmIN9bP6URhMZ5fq/50Gfaq9WKo+erC+l9Zg0CNyWRDtHmnaOyHCvMhqBmPHjJiKxBA32nusK2xvc7cJvDsKPq+hd0fcwUFD2rjrL7ZmxUVSGCUjterxOw5oDct1RKiyxOgfXa3dN5sW1RcH0D+R/JIxtH841W5Mv8/5aC7yK9zMqTGLQfG9e5jvR3hIbLHitWmsvJpBpZfTkA4MOzB/f2uzBI89MiovI6qLAc+cDOO3mYrWdP9DBeEchCBddMo6GFkcXfEQ1hMkEyDK1A6zSbb2SmIVt4fQM5MtDfQJQFZlPXcy+ms6XSdhJDkUFkZ3t9jpLXiabw0M7BkceZGk6l854TPI780WjJIIA==\\\",\\\"template\\\":\\\"https://t.chartboost.com/base_templates/html/mraid-iframe-open-df82555530.html\\\",\\\"trackingId\\\":\\\"23a79da8-c861-418c-b67c-8d44a108a069\\\"}}],\\\"seat\\\":\\\"Aarki\\\"}],\\\"cur\\\":\\\"USD\\\"}\", \"status\": 200}]}, \"resolvedrequest\": {\"id\": \"cac2faea23eeecc135bf4e678116c0c339fa915f\", \"imp\": [{\"id\": \"cac2faea23eeecc135bf4e678116c0c339fa915f\", \"banner\": {\"w\": 1080, \"h\": 2076, \"btype\": [4], \"battr\": [1, 2], \"pos\": 7, \"topframe\": 1, \"ext\": {\"placementtype\": \"interstitial\", \"playableonly\": false, \"allowscustomclosebutton\": true}}, \"video\": {\"mimes\": [\"video\\\\/mp4\"], \"minduration\": 5, \"maxduration\": 30, \"protocols\": [1, 2, 3], \"w\": 1080, \"h\": 2076, \"placement\": 5, \"linearity\": 1, \"skip\": 1, \"battr\": [1, 2], \"delivery\": [1, 2], \"pos\": 7, \"companiontype\": [1, 3], \"ext\": {\"placementtype\": \"interstitial\"}}, \"displaymanager\": \"Chartboost-Android-SDK\", \"displaymanagerver\": \"7.3.0\", \"instl\": 1, \"tagid\": \"CBInterstitial\", \"bidfloor\": 0.1, \"secure\": 1, \"ext\": {\"chartboost\": {\"app_id\": \"5a4e797538a5f00cf60738d6\", \"app_signature\": \"d29d75ce6213c746ba986f464e2b4a510be40399\"}}}], \"app\": {\"id\": \"5a4e797538a5f00cf60738d6\", \"name\": \"Super Mario Run\", \"bundle\": \"com.chartboost.heliumsdk.sampleapp\", \"storeurl\": \"market://details?id=com.nintendo.zara\", \"publisher\": {\"id\": \"596e66c543150f7e014a6660\", \"name\": \"Nintendo Co., Ltd.\"}}, \"device\": {\"ua\": \"Chartboost-Android-SDK  7.3.0\", \"geo\": {\"lat\": 40.5005, \"lon\": -3.66739, \"type\": 2, \"country\": \"ES\", \"region\": \"Madrid\", \"city\": \"Madrid\", \"zip\": \"28001\"}, \"lmt\": 1, \"ip\": \"88.6.2.104\", \"devicetype\": 4, \"make\": \"samsung\", \"model\": \"SM-G950F\", \"os\": \"Android\", \"osv\": \"8.0.0\", \"h\": 2076, \"w\": 1080, \"pxratio\": 480, \"language\": \"English\", \"connectiontype\": 2, \"ifa\": \"828a1342-7c12-4011-a330-49e766e5fb1d\"}, \"user\": {\"geo\": {\"lat\": 40.5005, \"lon\": -3.66739, \"type\": 2, \"country\": \"ES\", \"region\": \"Madrid\", \"city\": \"Madrid\", \"zip\": \"28001\"}}, \"test\": 1, \"at\": 1, \"tmax\": 750, \"cur\": [\"USD\"], \"regs\": {\"ext\": {\"gdpr\": 0}}, \"ext\": {\"prebid\": {\"targeting\": {}}}}}, \"responsetimemillis\": {\"chartboost\": 451}}}"
    private val resultsMock = mockk<(LoadResult) -> Unit>()
    private val cacheAssets = JSONObject("{\"testKey\":\"testValue\"}")
    private val eventTrackerMock = relaxedMockk<EventTrackerExtensions>()
    private val endpointRepositoryMock =
        mockk<EndpointRepository> {
            every { getEndPointUrl(any()) } answers { firstArg<EndpointRepository.EndPoint>().defaultUrl }
        }

    private val adUnitMock = AdUnit()
    private val appRequest = AppRequest(1, "location", null, null, adUnitMock)
    private val params = LoadParams(appRequest, true, 0, 0)
    private val appRequestBidResponse =
        AppRequest(
            1,
            "location",
            bidResponse,
            AdUnitBannerData(mockk<ViewGroup>(), 1, 1),
            adUnitMock,
        )
    private val paramsOrtb = LoadParams(appRequestBidResponse, true, 0, 0)

    private val adLoaderInterstitial =
        AdLoaderImpl(
            interstitialTraits,
            fileCacheMock,
            requestBodyBuilderMock,
            networkServiceMock,
            adUnitParserMock,
            openRTBAdUnitParser,
            openMeasurementManagerMock,
            eventTracker = eventTrackerMock,
            endpointRepository = endpointRepositoryMock,
        )

    private val adLoaderRewarded =
        AdLoaderImpl(
            rewardedTraits,
            fileCacheMock,
            requestBodyBuilderMock,
            networkServiceMock,
            adUnitParserMock,
            openRTBAdUnitParser,
            openMeasurementManagerMock,
            eventTracker = eventTrackerMock,
            endpointRepository = endpointRepositoryMock,
        )

    private val adLoaderBanner =
        AdLoaderImpl(
            bannerTraits,
            fileCacheMock,
            requestBodyBuilderMock,
            networkServiceMock,
            adUnitParserMock,
            openRTBAdUnitParser,
            openMeasurementManagerMock,
            eventTracker = eventTrackerMock,
            endpointRepository = endpointRepositoryMock,
        )

    @Before
    fun setup() {
        every { adUnitParserMock.parse(any()) } returns adUnitMock
        every { openRTBAdUnitParser.parse(any(), any()) } returns adUnitMock
        every { resultsMock.invoke(any()) }.answers { }
        every { requestBodyBuilderMock.build() }.returns(requestBodyFieldsMock)
        every { configurationBodyFieldsMock.webViewVersion }.returns("1")
        every { configurationBodyFieldsMock.webViewEnabled }.returns(true)
        every { requestBodyFieldsMock.session } returns sessionBodyFieldsMock
        every { requestBodyFieldsMock.configurationFields } returns configurationBodyFieldsMock
        every { requestBodyFieldsMock.deviceBodyFields } returns deviceBodyFieldsMock
        every { requestBodyFieldsMock.identityBodyFields } returns identityBodyFieldsMock
        every { requestBodyFieldsMock.reachabilityBodyFields } returns reachabilityBodyFieldsMock
        every { requestBodyFieldsMock.ortbDeviceType } returns 1
        every { requestBodyFieldsMock.getPrivacyBodyFields() } returns privacyBodyFieldsMock
        every { reachabilityBodyFieldsMock.openRTBConnectionType }.returns(NetworkType.UNKNOWN)
        every { identityBodyFieldsMock.gaid }.returns("gaid")
        every { identityBodyFieldsMock.setId }.returns("setid")
        every { identityBodyFieldsMock.setIdScope }.returns(1)
        every { identityBodyFieldsMock.trackingState }.returns(TrackingState.TRACKING_ENABLED)
        every { deviceBodyFieldsMock.deviceWidth }.returns(1)
        every { deviceBodyFieldsMock.deviceHeight }.returns(1)
        every { sessionBodyFieldsMock.interstitialImpressionCounter }.returns(1)
        every { sessionBodyFieldsMock.rewardedImpressionCounter }.returns(1)
        every { sessionBodyFieldsMock.bannerImpressionCounter }.returns(1)
        every { openMeasurementManagerMock.isOmSdkEnabled() } returns true
        every { openMeasurementManagerMock.getOmidPartner() } returns
            Partner.createPartner(
                "chartboost",
                "9.3.0",
            )
        every { fileCacheMock.webViewCacheAssets } returns JSONObject()
        every { networkServiceMock.submit(any<CBRequest>()) } just Runs
    }

    @Test
    fun `load ad interstitial success`() {
        val resultSlot = CapturingSlot<LoadResult>()
        val v2GetResponse =
            WebViewGetResponseBuilder.interstitialReturned()
                .withHtmlBodyElement(AssetDescriptor.baseTemplate2e34e6)
                .build()

        val adUnit = AdUnitParser(base64WrapperMock).parse(v2GetResponse)
        adUnitMock.cgn = adUnit.cgn
        val requestCaptor = CapturingSlot<CBNetworkRequest<CBWebViewRequest>>()
        adLoaderInterstitial.loadAd(params, resultsMock)
        verify(exactly = 1) { networkServiceMock.submit(capture(requestCaptor)) }
        val request = requestCaptor.captured as CBWebViewRequest
        assertNotNull(request)
        assertEquals(
            EndpointRepository.EndPoint.INTERSTITIAL_GET.defaultUrl.toString(),
            request.uri,
        )
        adLoaderInterstitial.onSuccess(request, v2GetResponse)
        io.mockk.verify(exactly = 1) { resultsMock.invoke(capture(resultSlot)) }
        io.mockk.verify(exactly = 1) { openMeasurementManagerMock.isOmSdkEnabled() }
        io.mockk.verify(exactly = 1) { openMeasurementManagerMock.getOmidPartner() }
        assertEquals(adUnit.cgn, resultSlot.captured.adUnit?.cgn)
        assertNull(resultSlot.captured.error)
    }

    @Test
    fun `load ad interstitial empty ad unit`() {
        every { configurationBodyFieldsMock.webViewEnabled }.returns(false)
        val resultSlot = CapturingSlot<LoadResult>()
        val requestCaptor = CapturingSlot<CBNetworkRequest<CBWebViewRequest>>()
        adLoaderInterstitial.loadAd(params, resultsMock)
        verify(exactly = 1) { networkServiceMock.submit(capture(requestCaptor)) }
        val request = requestCaptor.captured as CBWebViewRequest
        assertNotNull(request)
        assertEquals(
            EndpointRepository.EndPoint.INTERSTITIAL_GET.defaultUrl.toString(),
            request.uri,
        )
        adLoaderInterstitial.onSuccess(request, JSONObject())
        io.mockk.verify(exactly = 1) { resultsMock.invoke(capture(resultSlot)) }
        assertNull(resultSlot.captured.adUnit)
        assertEquals(CBError.Internal.UNEXPECTED_RESPONSE, resultSlot.captured.error?.type)
    }

    @Test
    fun `load ad interstitial failure response`() {
        val resultSlot = CapturingSlot<LoadResult>()
        val requestCaptor = CapturingSlot<CBNetworkRequest<CBWebViewRequest>>()
        adLoaderInterstitial.loadAd(params, resultsMock)
        verify(exactly = 1) { networkServiceMock.submit(capture(requestCaptor)) }
        val request = requestCaptor.captured as CBWebViewRequest
        assertNotNull(request)
        assertEquals(
            EndpointRepository.EndPoint.INTERSTITIAL_GET.defaultUrl.toString(),
            request.uri,
        )
        adLoaderInterstitial.onFailure(
            request,
            CBError(CBError.Internal.INVALID_RESPONSE, "test error"),
        )
        io.mockk.verify(exactly = 1) { resultsMock.invoke(capture(resultSlot)) }
        assertNull("", resultSlot.captured.adUnit)
        assertEquals(CBError.Internal.INVALID_RESPONSE, resultSlot.captured.error?.type)
    }

    @Test
    fun `load ad rewarded success`() {
        val requestCaptor = CapturingSlot<CBNetworkRequest<CBWebViewRequest>>()
        adLoaderRewarded.loadAd(params, resultsMock)
        verify(exactly = 1) { networkServiceMock.submit(capture(requestCaptor)) }
        val request = requestCaptor.captured
        assertNotNull(request)
        assertEquals(
            EndpointRepository.EndPoint.REWARDED_GET.defaultUrl.toString(),
            request.uri,
        )
    }

    @Test
    fun `load ad banner success`() {
        val requestCaptor = CapturingSlot<CBNetworkRequest<CBWebViewRequest>>()
        adLoaderBanner.loadAd(params, resultsMock)
        verify(exactly = 1) { networkServiceMock.submit(capture(requestCaptor)) }
        val request = requestCaptor.captured
        assertNotNull(request)
        assertEquals("https://da.chartboost.com/auction/sdk/banner", request.uri)
    }

    @Test
    fun `it dispatches a web view request, parses the response and calls onSuccess callback`() {
        every { fileCacheMock.webViewCacheAssets } returns cacheAssets
        every { networkServiceMock.submit(any<CBRequest>()) } answers { invocation ->
            val cbRequest = firstArg<CBRequest>()
            cbRequest.callback.onSuccess(
                cbRequest,
                WebViewGetResponseBuilder.interstitialReturned()
                    .withHtmlBodyElement(AssetDescriptor.baseTemplate33cda9)
                    .build(),
            )
        }

        var result: LoadResult? = null
        adLoaderInterstitial.loadAd(
            params,
        ) { result = it }

        val request = CapturingSlot<CBRequest>()
        verify { networkServiceMock.submit(capture(request)) }
        request.captured.apply {
            val bodyJson = body.get("ad") as JSONObject
            assertEquals(true, bodyJson.get("cache"))
            assertEquals(cacheAssets, bodyJson.get("cache_assets"))
            assertEquals(1, bodyJson.get("imp_depth"))
            assertEquals("location", bodyJson.get("location"))
            assertEquals(
                EndpointRepository.EndPoint.INTERSTITIAL_GET.defaultValue,
                path,
            )
            assertTrue(checkStatusInResponseBody)
            assertEquals("POST", method.toString())
            assertEquals(Priority.NORMAL, priority)
            assertEquals(CBNetworkRequest.Status.QUEUED, status.get())
            val sdkJson = body.get("sdk") as JSONObject
            assertEquals("chartboost", sdkJson.get("omidpn"))
            assertEquals("9.3.0", sdkJson.get("omidpv"))
        }
        result?.apply {
            assertNotNull(adUnit)
            assertNull(error)
            assertEquals(0, readDataNs)
            assertEquals(0, requestResponseCodeNs)
        }
    }

    @Test
    fun `it dispatches a web view request, parses the response and calls onSuccess callback when om is disabled`() {
        every { openMeasurementManagerMock.isOmSdkEnabled() } returns false
        every { fileCacheMock.webViewCacheAssets } returns cacheAssets
        every { networkServiceMock.submit(any<CBRequest>()) } answers { invocation ->
            val cbRequest = firstArg<CBRequest>()
            cbRequest.callback.onSuccess(
                cbRequest,
                WebViewGetResponseBuilder.interstitialReturned()
                    .withHtmlBodyElement(AssetDescriptor.baseTemplate33cda9)
                    .build(),
            )
        }

        var result: LoadResult? = null
        adLoaderInterstitial.loadAd(
            params,
        ) { result = it }

        val request = CapturingSlot<CBRequest>()
        verify { networkServiceMock.submit(capture(request)) }
        request.captured.apply {
            val bodyJson = body.get("ad") as JSONObject
            assertEquals(true, bodyJson.get("cache"))
            assertEquals(cacheAssets, bodyJson.get("cache_assets"))
            assertEquals(1, bodyJson.get("imp_depth"))
            assertEquals("location", bodyJson.get("location"))
            assertEquals(
                EndpointRepository.EndPoint.INTERSTITIAL_GET.defaultValue,
                path,
            )
            assertTrue(checkStatusInResponseBody)
            assertEquals("POST", method.toString())
            assertEquals(Priority.NORMAL, priority)
            assertEquals(CBNetworkRequest.Status.QUEUED, status.get())
            try {
                body.get("sdk") as JSONObject
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
        result?.apply {
            assertNotNull(adUnit)
            assertNull(error)
            assertEquals(0, readDataNs)
            assertEquals(0, requestResponseCodeNs)
        }
    }

    @Test
    fun `it calls onFailure callback when it fails to parse the response`() {
        every { configurationBodyFieldsMock.webViewEnabled }.returns(false)
        every { fileCacheMock.webViewCacheAssets } returns cacheAssets
        every { networkServiceMock.submit(any<CBRequest>()) } answers { invocation ->
            val cbRequest = firstArg<CBRequest>()
            cbRequest.callback.onSuccess(
                cbRequest,
                JSONObject(),
            )
        }

        var result: LoadResult? = null
        adLoaderInterstitial.loadAd(
            params,
        ) { result = it }

        result?.apply {
            assertNull(adUnit)
            assertNotNull(error)
            assertEquals(0, readDataNs)
            assertEquals(0, requestResponseCodeNs)
        }
    }

    @Test
    fun `it calls onFailure callback when the request fails`() {
        every { fileCacheMock.webViewCacheAssets } returns cacheAssets
        every { networkServiceMock.submit(any<CBRequest>()) } answers { invocation ->
            val cbRequest = firstArg<CBRequest>()
            cbRequest.callback.onFailure(
                cbRequest,
                CBError(CBError.Internal.INVALID_RESPONSE, "invalid response"),
            )
        }

        var result: LoadResult? = null
        adLoaderInterstitial.loadAd(
            params,
        ) { result = it }

        result?.apply {
            assertNull(adUnit)
            assertNotNull(error)
            assertEquals(0, readDataNs)
            assertEquals(0, requestResponseCodeNs)
        }
    }

    @Test
    fun `it dispatches an ORTB request, parses the response and calls onSuccess callback`() {
        val request = CapturingSlot<CBRequest>()
        every { networkServiceMock.submit(any<CBRequest>()) } answers { invocation ->
            val cbRequest = firstArg<CBRequest>()
            cbRequest.callback.onSuccess(
                cbRequest,
                JSONObject(bidResponse),
            )
        }

        var result: LoadResult? = null
        adLoaderBanner.loadAd(
            paramsOrtb,
        ) { result = it }

        verify { networkServiceMock.submit(capture(request)) }
        request.captured.apply {
            assertEquals("/auction/sdk/banner", path)
            assertFalse(checkStatusInResponseBody)
            assertEquals("POST", method.toString())
            assertEquals(Priority.NORMAL, priority)
            assertEquals(CBNetworkRequest.Status.QUEUED, status.get())
        }
        result?.apply {
            assertNotNull(adUnit)
            assertNull(error)
            assertEquals(0, readDataNs)
            assertEquals(0, requestResponseCodeNs)
        }
    }
}
