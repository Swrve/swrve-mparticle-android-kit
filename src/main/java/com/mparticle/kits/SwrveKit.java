package com.mparticle.kits;

import android.content.Context;

import java.util.List;
import java.util.Map;

/**
 *
 * This is an mParticle kit, used to extend the functionality of mParticle SDK. Most Kits are wrappers/adapters
 * to a 3rd party SDK, primarily used to map analogous public mParticle APIs onto a 3rd-party API/platform.
 *
 *
 * Follow the steps below to implement your kit:
 *
 *  - Edit ./build.gradle to add any necessary dependencies, such as your company's SDK
 *  - Rename this file/class, using your company name as the prefix, ie "AcmeKit"
 *  - View the javadocs to learn more about the KitIntegration class as well as the interfaces it defines.
 *  - Choose the additional interfaces that you need and have this class implement them,
 *    ie 'AcmeKit extends KitIntegration implements KitIntegration.PushListener'
 *
 *  In addition to this file, you also will need to edit:
 *  - ./build.gradle (as explained above)
 *  - ./README.md
 *  - ./src/main/AndroidManifest.xml
 *  - ./consumer-proguard.pro
 */
public class SwrveKit extends KitIntegration implements KitIntegration.UserAttributeListener, KitIntegration.CommerceListener, KitIntegration.EventListener, KitIntegration.PushListener, KitIntegration.IdentityListener {

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        /** TODO: Initialize your SDK here
         * This method is analogous to Application#onCreate, and will be called once per app execution.
         *
         * If for some reason you can't start your SDK (such as settings are not present), you *must* throw an Exception
         *
         * If you forward any events on startup that are analagous to any mParticle messages types, return them here
         * as ReportingMessage objects. Otherwise, return null.
         */
        return null;
    }

    @Override
    public Object getInstance() {
        //TODO: return Swrve object
        return null;
    }


    @Override
    public String getName() {
        //TODO: Replace this with your company name
        return "Swrve";
    }

    @Override
    public void onSettingsUpdated(Map<String, String> settings) {
        //TODO: SwrveConfig changes?
    }



    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) {
        //TODO: handle commerce event
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) {
        //TODO: handle custom events
        return null;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> screenAttributes) {
        //TODO: handle screen view events
        return null;
    }

    @Override
    public boolean willHandlePushMessage(Intent intent) {
        //TODO: determine if push is for Swrve
        return false;
    }

    @Override
    public void onPushMessageReceived(Context context, Intent pushIntent) {
        //TODO: let Swrve SDK handle push message - if willHandlePushMessage returns true
    }

    @Override
    public boolean onPushRegistration(String instanceId, String senderId) {
        //TODO: handle push registration
        return false;
    }

    @Override
    public void onUserIdentified(MParticleUser mParticleUser) {
        //TODO: init sdk once mpid is supplied?
    }

    @Override
    public void onIncrementUserAttribute (String key, int incrementedBy, String value, FilteredMParticleUser user) {
        //TODO: user update for full state of user attributes
    }

    @Override
    public void onRemoveUserAttribute(String key, FilteredMParticleUser user) {
        //TODO: update user property to blank
    }

    @Override
    public void onSetUserAttribute(String key, Object value, FilteredMParticleUser user) {
        //TODO: user update for key value pair
    }

    @Override
    public void onSetUserAttributeList(String attributeKey, List<String> attributeValueList, FilteredMParticleUser user) {
        //TODO: user update for key, with comma-separated list for value
    }

    @Override
    public void onSetAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, FilteredMParticleUser user) {
        //TODO: user update for full state of user attributes
    }

    @Override
    public boolean supportsAttributeLists() {
        return true;
    }


    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        //TODO: Disable or enable your SDK when a user opts out.
        //TODO: If your SDK can not be opted out of, return null
        ReportingMessage optOutMessage = new ReportingMessage(this, ReportingMessage.MessageType.OPT_OUT, System.currentTimeMillis(), null);
        return null;
    }

    @Override
    protected void onKitDestroy() {
        //TODO: clear Swrve storage
    }
}