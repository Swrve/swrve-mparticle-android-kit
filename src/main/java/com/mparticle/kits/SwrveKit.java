package com.mparticle.kits;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.Logger;
import com.swrve.sdk.SwrveIdentityResponse;
import com.swrve.sdk.SwrveInitMode;
import com.swrve.sdk.SwrveNotificationConfig;
import com.swrve.sdk.SwrvePushServiceDefault;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This is an mParticle kit, used to extend the functionality of mParticle SDK. Most Kits are wrappers/adapters
 * to a 3rd party SDK, primarily used to map analogous public mParticle APIs onto a 3rd-party API/platform.
 * <br>
 * <br>
 * Follow the steps below to implement your kit:
 * <br>
 * - Edit ./build.gradle to add any necessary dependencies, such as your company's SDK
 * - Rename this file/class, using your company name as the prefix, ie "AcmeKit"
 * - View the javadocs to learn more about the KitIntegration class as well as the interfaces it defines.
 * - Choose the additional interfaces that you need and have this class implement them,
 * ie 'AcmeKit extends KitIntegration implements KitIntegration.PushListener'
 * <br>
 * In addition to this file, you also will need to edit:
 * - ./build.gradle (as explained above)
 * - ./README.md
 * - ./src/main/AndroidManifest.xml
 * - ./consumer-proguard.pro
 */
public class SwrveKit extends KitIntegration implements KitIntegration.UserAttributeListener, KitIntegration.CommerceListener, KitIntegration.EventListener, KitIntegration.PushListener, KitIntegration.IdentityListener {

    public static final String SWRVE_MPARTICLE_VERSION_NUMBER = "3.0.0";
    public static final String SWRVE_PUSH_OPT_IN_EVENT_NAME = "swrve_push_opt_in";

    private SwrveInitMode swrveInitMode = SwrveInitMode.MANAGED;
    protected String userIdType = "MPID";

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {

        createSwrveInstance(settings, context);

        List<ReportingMessage> messageList = new LinkedList<>();
        messageList.add(new ReportingMessage(this, ReportingMessage.MessageType.SESSION_START, System.currentTimeMillis(), null));
        return messageList;
    }

    private void createSwrveInstance(Map<String, String> settings, Context context) {
        int app_id = Integer.parseInt(settings.get("app_id"));
        String api_key = settings.get("api_key");

        SwrveStack swrveStack = SwrveStack.US; // default to US
        String stack = settings.get("swrve_stack");
        if (stack.equals("EU")) {
            swrveStack = SwrveStack.EU;
        }

        String init_mode = settings.get("initialization_mode");
        if (init_mode.equals("AUTO")) {
            swrveInitMode = SwrveInitMode.AUTO;
        }

        if (swrveInitMode == SwrveInitMode.AUTO) {
            userIdType = settings.get("external_user_id");
        } else if (swrveInitMode == SwrveInitMode.MANAGED) {
            userIdType = settings.get("swrve_user_id");
        }

        SwrveConfig config = new SwrveConfig();
        SwrveNotificationConfig notificationConfig = getNotificationConfig(context);
        config.setNotificationConfig(notificationConfig);
        config.setInitMode(swrveInitMode);
        config.setSelectedStack(swrveStack);

        SwrveSDK.createInstance(((Application) context.getApplicationContext()), app_id, api_key, config);
    }

    private SwrveNotificationConfig getNotificationConfig(Context context) {
        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Resources resources = context.getResources();
            int channelIdResourceId = getResourceId("swrve_notification_channel_id", "string", context);
            String channelId = resources.getString(channelIdResourceId);
            int channelNameResourceId = getResourceId("swrve_notification_channel_name", "string", context);
            String channelName = resources.getString(channelNameResourceId);
            channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            if (context.getSystemService(Context.NOTIFICATION_SERVICE) != null) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }
        }

        int iconDrawableId = getResourceId("swrve_push_icon_drawable", "drawable", context);
        int iconMaterialDrawableId = getResourceId("swrve_push_icon_material_drawable", "drawable", context);
        int largeIconDrawableId = getResourceId("swrve_push_large_icon_drawable", "drawable", context);
        int accentColorId = getResourceId("swrve_push_accent_color_hex", "string", context);
        String accentColorHex = context.getResources().getString(accentColorId);
        Class activityClass = getActivityClass(context);
        List<String> pushNotificationPermissionEvents = new ArrayList<>();
        pushNotificationPermissionEvents.add("other." + SWRVE_PUSH_OPT_IN_EVENT_NAME);
        SwrveNotificationConfig.Builder notificationConfig = new SwrveNotificationConfig.Builder(iconDrawableId, iconMaterialDrawableId, channel)
                .activityClass(activityClass)
                .largeIconDrawableId(largeIconDrawableId)
                .accentColorHex(accentColorHex)
                .pushNotificationPermissionEvents(pushNotificationPermissionEvents);
        return notificationConfig.build();
    }

    private Class getActivityClass(Context context) {
        Class clazz = null;
        int notificationActivityClassId = getResourceId("swrve_notification_activity_class_name", "string", context);
        String notificationActivityClassName = context.getResources().getString(notificationActivityClassId);
        try {
            clazz = Class.forName(notificationActivityClassName);
        } catch (ClassNotFoundException e) {
            Logger.error(e, "SwrveKit error finding Activity to start from push notification");
        }
        return clazz;
    }

    private int getResourceId(String variableName, String resourceName, Context context) {
        int resourceId = -1;
        String packageName = context.getPackageName();
        try {
            resourceId = context.getResources().getIdentifier(variableName, resourceName, packageName);
        } catch (Exception e) {
            Logger.error(e, "SwrveKit error getting resource id:" + resourceName);
        }
        return resourceId;
    }

    @Override
    public Object getInstance() {
        return SwrveSDK.getInstance();
    }

    @Override
    public String getName() {
        return "Swrve";
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        ReportingMessage optOutMessage = new ReportingMessage(this, ReportingMessage.MessageType.OPT_OUT, System.currentTimeMillis(), null);
        return null;
    }

    //  start interface CommerceListener
    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) {
        //TODO: handle commerce event
        List<ReportingMessage> messageList = new LinkedList<>();
        String currency = "USD";
        if (event.getCurrency() != null) {
            currency = event.getCurrency();
        }

        if (event.getProductAction() != Product.PURCHASE) {
            //TODO: handle non-purchase commerce events (e.g. add to cart, add to wishlist, etc.)
            return null;
        }
        messageList.add(ReportingMessage.fromEvent(this, event));
        List<Product> products = event.getProducts();
        for (Product product : products) {
            int quantity = (int) product.getQuantity();
            SwrveSDK.iap(quantity, product.getSku(), product.getUnitPrice(), currency);
        }
        return messageList;
    }

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, BigDecimal valueTotal, String eventName, Map<String, String> contextInfo) {
        return null; //do nothing
    }
    //  end interface CommerceListener

    //  start interface EventListener
    @Override
    public List<ReportingMessage> leaveBreadcrumb(String breadcrumb) {
        return null; //do nothing
    }

    @Override
    public List<ReportingMessage> logError(String message, Map<String, String> errorAttributes) {
        return null; //do nothing
    }

    @Override
    public List<ReportingMessage> logException(Exception exception, Map<String, String> exceptionAttributes, String message) {
        return null; //do nothing
    }

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) {
        Map<String, String> customAttributes = getCustomAttributes(event.getCustomAttributes());
        if (event.getEventType() == MParticle.EventType.Other
                && customAttributes != null
                && customAttributes.containsKey("given_currency")
                && customAttributes.containsKey("given_amount")) {
            String givenCurrency = customAttributes.get("given_currency");
            double givenAmount = new Double(customAttributes.get("given_amount"));
            SwrveSDK.currencyGiven(givenCurrency, givenAmount);
        } else {
            String eventName = event.getEventType().toString().toLowerCase(Locale.ENGLISH) + "." + event.getEventName();
            SwrveSDK.event(eventName, customAttributes);
        }

        List<ReportingMessage> messageList = new LinkedList<>();
        messageList.add(ReportingMessage.fromEvent(this, event));
        return messageList;
    }

    private Map<String, String> getCustomAttributes(Map<String, Object> mpCustomAttributes) {
        Map<String, String> customAttributes = new HashMap<>();
        if (mpCustomAttributes != null) {
            for (Map.Entry<String, Object> entry : mpCustomAttributes.entrySet()) {
                if (entry.getValue() instanceof String) {
                    customAttributes.put(entry.getKey(), (String) entry.getValue());
                } else {
                    String value = String.valueOf(entry.getValue());
                    customAttributes.put(entry.getKey(), value);
                }
            }
        }
        return customAttributes;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> screenAttributes) {
        List<ReportingMessage> messageList = new LinkedList<>();
        SwrveSDK.event("screen_view" + "." + screenName, screenAttributes);
        messageList.add(new ReportingMessage(this, ReportingMessage.MessageType.SCREEN_VIEW, System.currentTimeMillis(), screenAttributes));
        return messageList;
    }
    //  end interface EventListener

    //  start interface PushListener
    @Override
    public boolean willHandlePushMessage(Intent intent) {
        Bundle extras = intent.getExtras();
        return (extras.containsKey("_p") || extras.containsKey("_sp")); //determine if push is from Swrve
    }

    @Override
    public void onPushMessageReceived(Context context, Intent pushIntent) {
        SwrvePushServiceDefault.handle(context, pushIntent); // let SwrveSDK handle push message - if willHandlePushMessage returns true
    }

    @Override
    public boolean onPushRegistration(String instanceId, String senderId) {
        SwrveSDK.setRegistrationId(instanceId);
        return true;
    }
    //  end interface PushListener

    //  start interface IdentityListener
    @Override
    public void onIdentifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
        Activity activity = super.getCurrentActivity().get();
        identityMethodsStart(activity, mParticleUser);
    }

    @Override
    public void onLoginCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
        Activity activity = super.getCurrentActivity().get();
        identityMethodsStart(activity, mParticleUser);
    }

    @Override
    public void onLogoutCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
        //do nothing
    }

    @Override
    public void onModifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest) {
        Activity activity = super.getCurrentActivity().get();
        identityMethodsStart(activity, mParticleUser);
    }

    @Override
    public void onUserIdentified(MParticleUser mParticleUser) {
        Activity activity = super.getCurrentActivity().get();
        identityMethodsStart(activity, mParticleUser);
    }
    //  end interface IdentityListener

    // start interface UserAttributeListener
    @Override
    public void onIncrementUserAttribute(String key, Number incrementedBy, String value, FilteredMParticleUser user) {
        Activity activity = getCurrentActivity().get();
        startSdkIfNotStarted(activity, user);

        Map<String, Object> attributes = user.getUserAttributes();
        Map<String, String> newAttributes = new HashMap<>();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            newAttributes.put(entry.getKey(), entry.getValue().toString());
        }
        SwrveSDK.userUpdate(newAttributes);
    }

    @Override
    public void onRemoveUserAttribute(String key, FilteredMParticleUser user) {
        Activity activity = super.getCurrentActivity().get();
        startSdkIfNotStarted(activity, user);

        Map<String, String> attributes = new HashMap<>();
        attributes.put(key, "");
        SwrveSDK.userUpdate(attributes);
    }

    @Override
    public void onSetUserAttribute(String key, Object value, FilteredMParticleUser user) {
        Activity activity = super.getCurrentActivity().get();
        startSdkIfNotStarted(activity, user);

        Map<String, String> attributes = new HashMap<>();
        attributes.put(key, value.toString());
        SwrveSDK.userUpdate(attributes);
    }

    @Override
    public void onSetUserAttributeList(String attributeKey, List<String> attributeValueList, FilteredMParticleUser user) {
        //do nothing
    }

    @Override
    public void onSetUserTag(String key, FilteredMParticleUser user) {
        //do nothing
    }

    @Override
    public void onConsentStateUpdated(ConsentState oldState, ConsentState newState, FilteredMParticleUser user) {
        //do nothing
    }

    @Override
    public void onSetAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, FilteredMParticleUser user) {
        Activity activity = super.getCurrentActivity().get();
        startSdkIfNotStarted(activity, user);
        SwrveSDK.userUpdate(userAttributes);
    }

    @Override
    public boolean supportsAttributeLists() {
        return false;
    }
    // end interface UserAttributeListener

    private void identityMethodsStart(Activity activity, MParticleUser user) {
        if (activity != null && swrveInitMode == SwrveInitMode.MANAGED && !SwrveSDK.isStarted()) {
            startSwrveSDK(activity, user);
        }
        if (swrveInitMode == SwrveInitMode.AUTO) {
            identifySwrveUser(user);
        }
    }

    private void startSwrveSDK(Activity activity, MParticleUser user) {
        String user_id;
        if (userIdType.equals("MPID")) {
            user_id = Long.toString(user.getId());
        } else {
            user_id = user.getUserIdentities().get(MParticle.IdentityType.CustomerId);
        }
        if (user_id != null) {
            SwrveSDK.start(activity, user_id);
            Logger.debug("SwrveKit: Swrve SDK started in MANAGED mode with swrve user id: " + user_id);
            Map<String, String> version = new HashMap<>();
            version.put("swrve.mparticle_android_integration_version", SWRVE_MPARTICLE_VERSION_NUMBER);
            SwrveSDK.userUpdate(version);
            SwrveSDK.sendQueuedEvents();
        }
    }

    private void identifySwrveUser(MParticleUser user) {
        String external_id;
        if (userIdType.equals("MPID")) {
            external_id = Long.toString(user.getId());
        } else {
            MParticle.IdentityType identityType;
            switch (userIdType) {
                case "Customer ID":
                    identityType = MParticle.IdentityType.CustomerId;
                    break;
                case "Other":
                    identityType = MParticle.IdentityType.Other;
                    break;
                case "Other2":
                    identityType = MParticle.IdentityType.Other2;
                    break;
                case "Other3":
                    identityType = MParticle.IdentityType.Other3;
                    break;
                case "Other4":
                    identityType = MParticle.IdentityType.Other4;
                    break;
                case "Other5":
                    identityType = MParticle.IdentityType.Other5;
                    break;
                case "Other6":
                    identityType = MParticle.IdentityType.Other6;
                    break;
                case "Other7":
                    identityType = MParticle.IdentityType.Other7;
                    break;
                case "Other8":
                    identityType = MParticle.IdentityType.Other8;
                    break;
                case "Other9":
                    identityType = MParticle.IdentityType.Other9;
                    break;
                case "Other10":
                    identityType = MParticle.IdentityType.Other10;
                    break;
                default:
                    identityType = MParticle.IdentityType.CustomerId;
            }
            external_id = user.getUserIdentities().get(identityType);
        }
        if (external_id != null) {
            final String ext_id = external_id;
            SwrveSDK.identify(ext_id, new SwrveIdentityResponse() {
                @Override
                public void onSuccess(String status, String swrveId) {
                    Logger.debug("SwrveKit: User successfully identified with Swrve.\tExternal user id: " + ext_id + "\tSwrve user id: " + swrveId);
                }

                @Override
                public void onError(int responseCode, String errorMessage) {
                    Logger.error("SwrveKit: User failed to identify with Swrve.\tResponse Code: " + responseCode + "\tMessage: " + errorMessage);
                }
            });
        }
    }

    private void startSdkIfNotStarted(Activity activity, FilteredMParticleUser user) {
        String user_id;
        if (userIdType.equals("MPID")) {
            user_id = Long.toString(user.getId());
        } else {
            user_id = user.getUserIdentities().get(MParticle.IdentityType.CustomerId);
        }
        if (user_id != null) {
            SwrveSDK.start(activity, user_id);
            Logger.debug("SwrveKit: Swrve SDK started in MANAGED mode with swrve user id: " + user_id);
            Map<String, String> version = new HashMap<String, String>();
            version.put("swrve.mparticle_android_integration_version", SWRVE_MPARTICLE_VERSION_NUMBER);
            SwrveSDK.userUpdate(version);
            SwrveSDK.sendQueuedEvents();
        }
    }

}
