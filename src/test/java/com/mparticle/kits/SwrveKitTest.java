package com.mparticle.kits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;
import com.swrve.sdk.ISwrve;
import com.swrve.sdk.ISwrveBase;
import com.swrve.sdk.Swrve;
import com.swrve.sdk.SwrveInitMode;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveNotificationConfig;
import com.swrve.sdk.SwrveSDKBase;
import com.swrve.sdk.config.SwrveStack;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.ReflectionHelpers;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S)
@TargetApi(Build.VERSION_CODES.S)
public class SwrveKitTest {

    private SwrveKit swrveKit;

    @Before
    public void setUp() {
        SwrveLogger.setLogLevel(Log.VERBOSE);
        ShadowLog.stream = System.out;
        swrveKit = new SwrveKit();

        KitConfiguration kitConfigurationMock = mock(KitConfiguration.class);
        doReturn(9876).when(kitConfigurationMock).getKitId();
        swrveKit.setConfiguration(kitConfigurationMock);

        KitManagerImpl kitManagerMock = mock(KitManagerImpl.class);
        Activity activityMock = mock(Activity.class);
        WeakReference<Activity> activityWeakReference = new WeakReference<>(activityMock);
        doReturn(activityWeakReference).when(kitManagerMock).getCurrentActivity();
        swrveKit.setKitManager(kitManagerMock);
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(SwrveSDKBase.class, "instance", null);
    }

    @Test
    public void testGetName() {
        assertEquals("Swrve", swrveKit.getName());
    }

    @Test
    public void testKnownIntegrations() {
        KitIntegrationFactory factory = new KitIntegrationFactory();
        Map<Integer, String> integrations = factory.getKnownIntegrations();
        assertTrue(integrations.containsKey(MParticle.ServiceProviders.SWRVE));
        assertEquals("com.mparticle.kits.SwrveKit", integrations.get(MParticle.ServiceProviders.SWRVE));
    }

    @Test
    public void testOnKitCreateInvalidSettings() {
        Exception e = null;
        try {
            Map settings = new HashMap<>();
            settings.put("fake setting", "fake");
            swrveKit.onKitCreate(settings, mock(Context.class));
        } catch (Exception ex) {
            e = ex;
        }
        assertNotNull(e);
    }

    @Test
    public void testOnKitCreateUSAUTO() {

        Map<String, String> settings = getFakeSettings(SwrveStack.US, "AUTO");
        swrveKit.onKitCreate(settings, ApplicationProvider.getApplicationContext());

        assertNotNull(swrveKit.getInstance());
        assertTrue(swrveKit.getInstance() instanceof Swrve);
        Swrve swrve = (Swrve) swrveKit.getInstance();
        assertEquals(123, swrve.getAppId());
        assertEquals("some_api_key", swrve.getApiKey());
        assertEquals(SwrveStack.US, swrve.getConfig().getSelectedStack());
        assertEquals(SwrveInitMode.AUTO, swrve.getConfig().getInitMode());
        assertEquals("external_user_id", swrveKit.userIdType);

        SwrveNotificationConfig notificationConfig = swrve.getConfig().getNotificationConfig();
        assertEquals("123", notificationConfig.getDefaultNotificationChannel().getId());
        assertEquals("Notification Channel", notificationConfig.getDefaultNotificationChannel().getName());
        assertEquals("#3949AB", notificationConfig.getAccentColorHex());
        assertEquals(com.mparticle.kits.MainActivity.class, notificationConfig.getActivityClass());
        List<String> pushNotificationPermissionEvents = new ArrayList<>();
        pushNotificationPermissionEvents.add("other.swrve_push_opt_in");
        assertEquals(pushNotificationPermissionEvents, notificationConfig.getPushNotificationPermissionEvents());
    }

    @Test
    public void testOnKitCreateEUMANAGED() {

        Map<String, String> settings = getFakeSettings(SwrveStack.EU, "MANAGED");
        swrveKit.onKitCreate(settings, ApplicationProvider.getApplicationContext());

        assertNotNull(swrveKit.getInstance());
        assertTrue(swrveKit.getInstance() instanceof Swrve);
        Swrve swrve = (Swrve) swrveKit.getInstance();
        assertEquals(123, swrve.getAppId());
        assertEquals("some_api_key", swrve.getApiKey());
        assertEquals(SwrveStack.EU, swrve.getConfig().getSelectedStack());
        assertEquals(SwrveInitMode.MANAGED, swrve.getConfig().getInitMode());
        assertEquals("swrve_user_id", swrveKit.userIdType);
    }

    @Test
    public void testLogCommerceEvent() {

        Map<String, String> settings = getFakeSettings(SwrveStack.US, "AUTO");
        swrveKit.onKitCreate(settings, ApplicationProvider.getApplicationContext());

        // replace swrve instance with a spy/mock/dummy
        ISwrveBase swrveMock = mock(ISwrveBase.class);
        ReflectionHelpers.setStaticField(SwrveSDKBase.class, "instance", swrveMock);

        Product product = new Product.Builder("Double Room - Econ Rate", "econ-1", 100.00)
                .quantity(4.0)
                .build();
        TransactionAttributes transactionAttributes = new TransactionAttributes("foo-transaction-id")
                .setRevenue(430.00)
                .setTax(30.00);
        CommerceEvent commerceEvent = new CommerceEvent.Builder(Product.PURCHASE, product).
                transactionAttributes(transactionAttributes)
                .build();
        swrveKit.logEvent(commerceEvent);

        verify(swrveMock, atLeast(1)).iap(4, "econ-1", 100.0d, "USD");
    }

    @Test
    public void testLogMPEvent() {

        Map<String, String> settings = getFakeSettings(SwrveStack.US, "AUTO");
        swrveKit.onKitCreate(settings, ApplicationProvider.getApplicationContext());

        // replace swrve instance with a spy/mock/dummy
        ISwrveBase swrveMock = mock(ISwrveBase.class);
        ReflectionHelpers.setStaticField(SwrveSDKBase.class, "instance", swrveMock);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("k1", "v1");
        attributes.put("k2", "v2");
        MPEvent mpEvent = new MPEvent.Builder("my_event_name", MParticle.EventType.Other)
                .customAttributes(attributes)
                .build();
        swrveKit.logEvent(mpEvent);

        verify(swrveMock, atLeast(1)).event("other.my_event_name", attributes);
    }

    @Test
    public void testLogScreen() {

        Map<String, String> settings = getFakeSettings(SwrveStack.US, "AUTO");
        swrveKit.onKitCreate(settings, ApplicationProvider.getApplicationContext());

        // replace swrve instance with a spy/mock/dummy
        ISwrveBase swrveMock = mock(ISwrveBase.class);
        ReflectionHelpers.setStaticField(SwrveSDKBase.class, "instance", swrveMock);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("k1", "v1");
        attributes.put("k2", "v2");
        swrveKit.logScreen("main", attributes);

        verify(swrveMock, atLeast(1)).event("screen_view.main", attributes);
    }

    @Test
    public void testOnPushRegistration() {

        Map<String, String> settings = getFakeSettings(SwrveStack.US, "AUTO");
        swrveKit.onKitCreate(settings, ApplicationProvider.getApplicationContext());

        // replace swrve instance with a spy/mock/dummy
        ISwrve swrveMock = mock(ISwrve.class);
        ReflectionHelpers.setStaticField(SwrveSDKBase.class, "instance", swrveMock);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("k1", "v1");
        attributes.put("k2", "v2");
        swrveKit.onPushRegistration("instance_id_a", "sender_id_a");

        verify(swrveMock, atLeast(1)).setRegistrationId("instance_id_a");
    }

    @Test
    public void testOnIncrementUserAttribute() {

        Map<String, String> settings = getFakeSettings(SwrveStack.US, "AUTO");
        swrveKit.onKitCreate(settings, ApplicationProvider.getApplicationContext());

        // replace swrve instance with a spy/mock/dummy
        ISwrve swrveMock = mock(ISwrve.class);
        ReflectionHelpers.setStaticField(SwrveSDKBase.class, "instance", swrveMock);

        FilteredMParticleUser filteredMParticleUser = mock(FilteredMParticleUser.class);
        Map<String, String> attributes = new HashMap<>();
        attributes.put("k1", "v1");
        attributes.put("k2", "v2");
        doReturn(attributes).when(filteredMParticleUser).getUserAttributes();
        swrveKit.onIncrementUserAttribute("a_key", 123, "a_value", filteredMParticleUser);

        verify(swrveMock, atLeast(1)).userUpdate(attributes);
    }

    @Test
    public void testOnRemoveUserAttribute() {

        Map<String, String> settings = getFakeSettings(SwrveStack.US, "AUTO");
        swrveKit.onKitCreate(settings, ApplicationProvider.getApplicationContext());

        // replace swrve instance with a spy/mock/dummy
        ISwrve swrveMock = mock(ISwrve.class);
        ReflectionHelpers.setStaticField(SwrveSDKBase.class, "instance", swrveMock);

        FilteredMParticleUser filteredMParticleUser = mock(FilteredMParticleUser.class);
        swrveKit.onRemoveUserAttribute("a_key", filteredMParticleUser);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("a_key", "");
        verify(swrveMock, atLeast(1)).userUpdate(attributes);
    }

    @Test
    public void testOnSetUserAttribute() {

        Map<String, String> settings = getFakeSettings(SwrveStack.US, "AUTO");
        swrveKit.onKitCreate(settings, ApplicationProvider.getApplicationContext());

        // replace swrve instance with a spy/mock/dummy
        ISwrve swrveMock = mock(ISwrve.class);
        ReflectionHelpers.setStaticField(SwrveSDKBase.class, "instance", swrveMock);

        FilteredMParticleUser filteredMParticleUser = mock(FilteredMParticleUser.class);
        swrveKit.onSetUserAttribute("a_key", "a_value", filteredMParticleUser);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("a_key", "a_value");
        verify(swrveMock, atLeast(1)).userUpdate(attributes);
    }

    @Test
    public void testOnSetAllUserAttributes() {

        Map<String, String> settings = getFakeSettings(SwrveStack.US, "AUTO");
        swrveKit.onKitCreate(settings, ApplicationProvider.getApplicationContext());

        // replace swrve instance with a spy/mock/dummy
        ISwrve swrveMock = mock(ISwrve.class);
        ReflectionHelpers.setStaticField(SwrveSDKBase.class, "instance", swrveMock);

        Map<String, String> userAttributes = new HashMap<>();
        userAttributes.put("k1", "v1");
        userAttributes.put("k2", "v2");

        List<String> userAttributeList = new ArrayList<>();
        userAttributeList.add("l1");
        Map<String, List<String>> userAttributesLists = new HashMap<>();
        userAttributesLists.put("k3", userAttributeList);


        FilteredMParticleUser filteredMParticleUser = mock(FilteredMParticleUser.class);
        swrveKit.onSetAllUserAttributes(userAttributes, userAttributesLists, filteredMParticleUser);

        verify(swrveMock, atLeast(1)).userUpdate(userAttributes);
    }

    private Map<String, String> getFakeSettings(SwrveStack stack, String initializationMode) {
        Map<String, String> settings = new HashMap<>();
        settings.put("app_id", "123");
        settings.put("api_key", "some_api_key");
        settings.put("swrve_stack", stack.toString());
        settings.put("initialization_mode", initializationMode);
        if (stack == SwrveStack.US) {
            settings.put("external_user_id", "external_user_id");
        } else if (stack == SwrveStack.EU) {
            settings.put("swrve_user_id", "swrve_user_id");
        }
        return settings;
    }
}
