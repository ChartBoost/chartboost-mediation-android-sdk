package com.chartboost.sdk.privacy.usecase;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import com.chartboost.sdk.privacy.model.Custom;
import com.chartboost.sdk.privacy.model.DataUseConsent;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class GetPrivacyListAsJsonUseCaseTest {

    private GetPrivacyListAsJsonUseCase usecase = new GetPrivacyListAsJsonUseCase();

    @Test
    public void getPrivacyListAsJsonTest() throws JSONException {
        DataUseConsent custom = new Custom("test", "test_consent");
        List<DataUseConsent> data = new ArrayList<>();
        data.add(custom);

        JSONObject json = usecase.execute(data);

        assertNotNull(json);
        String decodeConsent = json.getString("test");
        assertEquals(custom.getConsent(), decodeConsent);
    }

    @Test
    public void getPrivacyListAsJsonEmptyTest() {
        List<DataUseConsent> data = new ArrayList<>();
        JSONObject json = usecase.execute(data);
        assertNotNull(json);
        assertEquals(0, json.length());
    }
}
