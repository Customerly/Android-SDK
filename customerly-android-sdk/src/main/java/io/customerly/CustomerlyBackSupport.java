package io.customerly;

/*
 * Copyright (C) 2017 Customerly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.app.Application;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

@SuppressWarnings("unused")
@Deprecated
public class CustomerlyBackSupport {

    @SuppressWarnings("deprecation")
    @NonNull private static final CustomerlyBackSupport INSTANCE = new CustomerlyBackSupport();

    private CustomerlyBackSupport() { }

    /**
     * Call this method to obtain the reference to the Customerly SDK
     * @return The Customerly SDK instance reference
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    @NonNull public static CustomerlyBackSupport get() {
        return CustomerlyBackSupport.INSTANCE;
    }

    /**
     * Call this method to configure the SDK indicating the Customerly App ID before accessing it.<br>
     * Call this from your custom Application {@link Application#onCreate()}
     * @param pApplication The application class reference
     * @param pCustomerlyAppID The Customerly App ID found in your Customerly console
     */
    @Deprecated
    public static void configure(@NonNull Application pApplication, @NonNull String pCustomerlyAppID) {
        Customerly.configure(pApplication, pCustomerlyAppID);
    }

    /**
     * Call this method to configure the SDK indicating the Customerly App ID before accessing it.<br>
     * Call this from your custom Application {@link Application#onCreate()}<br>
     *     <br>
     * You can choose to ignore the widget_color provided by the Customerly web console for the action bar styling in support activities and use an app-local widget_color instead.
     * @param pApplication The application class reference
     * @param pCustomerlyAppID The Customerly App ID found in your Customerly console
     * @param pWidgetColor The custom widget_color. If Color.TRANSPARENT, it will be ignored
     */
    @Deprecated
    public static void configure(@NonNull Application pApplication, @NonNull String pCustomerlyAppID, @ColorInt int pWidgetColor) {
        Customerly.configure(pApplication, pCustomerlyAppID, pWidgetColor);
    }

    /**
     * Call this method to enable error logging in the Console.
     * Avoid to enable it in release app versions, the suggestion is to pass your.application.package.BuildConfig.DEBUG as parameter
     * @param pVerboseLogging true for enable logging, please pass your.application.package.BuildConfig.DEBUG
     */
    @Deprecated
    public void setVerboseLogging(boolean pVerboseLogging) {
        Customerly.setVerboseLogging(pVerboseLogging);
    }

    public interface Callback {
        /**
         * Implement this interface to obtain async success or failure response from {@link #registerUser(String)},
         * {@link #setCompany(HashMap)} or {@link #setAttributes(HashMap)}
         */
        void callback();
    }

    public interface Task {
        /**
         * @param successCallback To receive success async response
         * @return The Task itself for method chaining
         */
        @CheckResult @NonNull Task successCallback(@Nullable Callback successCallback);
        /**
         * @param failureCallback To receive failure async response
         * @return The Task itself for method chaining
         */
        @CheckResult @NonNull Task failureCallback(@Nullable Callback failureCallback);
        /**
         * Don't forget to call this method to start the task
         */
        void start();
    }

    private abstract class __Task implements Task{
        @Nullable Callback successCallback;
        @Nullable Callback failureCallback;
        /**
         * @param successCallback To receive success async response
         * @return The Task itself for method chaining
         */
        @CheckResult @Override @NonNull public Task successCallback(@Nullable Callback successCallback) {
            this.successCallback = successCallback;
            return this;
        }
        /**
         * @param failureCallback To receive failure async response
         * @return The Task itself for method chaining
         */
        @CheckResult @Override @NonNull public Task failureCallback(@Nullable Callback failureCallback) {
            this.failureCallback = failureCallback;
            return this;
        }
    }

    private class CallbackWrapper implements Function0<Unit> {
        @Nullable private final Callback callback;
        private CallbackWrapper(@Nullable Callback callback) {
            this.callback = callback;
        }
        @Override
        public Unit invoke() {
            if(this.callback != null) {
                this.callback.callback();
            }
            return null;
        }
    }

    public final class UpdateTask extends __Task {
        private UpdateTask() {
            super();
        }
        @Override public void start() {
            Customerly.update(new CallbackWrapper(this.successCallback), new CallbackWrapper(this.failureCallback));
        }
    }

    public final class RegisterUserTask extends __Task {
        @NonNull private final String email;
        @Nullable private String user_id, name;
        @Nullable private HashMap<String,Object> attributes, company;

        private RegisterUserTask(@NonNull String email) {
            super();
            this.email = email.trim();
        }
        /**
         * Optionally you can specify the user ID
         * @param user_id The ID of the user
         * @return The Task itself for method chaining
         */
        @CheckResult @NonNull public RegisterUserTask user_id(@Nullable String user_id) {
            if(user_id != null) {
                user_id = user_id.trim();
                if(user_id.length() != 0) {
                    this.user_id = user_id;
                }
            } else {
                this.user_id = null;
            }
            return this;
        }
        /**
         * Optionally you can specify the user name
         * @param name The name of the user
         * @return The Task itself for method chaining
         */
        @CheckResult @NonNull public RegisterUserTask name(@Nullable String name) {
            if(name != null) {
                name = name.trim();
                if (name.length() != 0) {
                    this.name = name;
                }
            } else {
                this.name = null;
            }
            return this;
        }
        /**
         * Optionally you can specify the user attributes
         * @param pAttributes The attributes of the user. Can contain only String, char, byte, int, long, float or double values
         * @return The Task itself for method chaining
         * @throws IllegalArgumentException if the attributes map check fails
         */
        @CheckResult @NonNull public RegisterUserTask attributes(@Nullable HashMap<String,Object> pAttributes) throws IllegalArgumentException {
            if(pAttributes != null) {
                Collection<Object> attrs = pAttributes.values();
                for (Object attr : attrs) {
                    if (attr instanceof String ||
                            attr instanceof Integer ||
                            attr instanceof Byte ||
                            attr instanceof Long ||
                            attr instanceof Double ||
                            attr instanceof Float ||
                            attr instanceof Character ||
                            attr instanceof Boolean) {
                        continue;
                    }
                    throw new IllegalArgumentException("Attributes HashMap can contain only Strings, int, float, long, double or char values");
                }
                this.attributes = pAttributes;
            } else {
                this.attributes = null;
            }
            return this;
        }
        /**
         * Optionally you can specify the user company
         * @param pCompany The company of the user. The map must contain a String value with key "company_id" containing to the Company ID and a String value with key "name" containing the Company name. Can contain only String, char, int, long, float or double values
         * @return The Task itself for method chaining
         * @throws IllegalArgumentException if the company map check fails
         */
        @CheckResult @NonNull public RegisterUserTask company(@Nullable HashMap<String,Object> pCompany) throws IllegalArgumentException{
            if(pCompany != null) {
                Collection<Object> attrs = pCompany.values();
                for(Object attr : attrs) {
                    if(     attr instanceof String ||
                            attr instanceof Integer ||
                            attr instanceof Byte ||
                            attr instanceof Long ||
                            attr instanceof Double ||
                            attr instanceof Float ||
                            attr instanceof Character ||
                            attr instanceof Boolean) {
                        continue;
                    }
                    throw new IllegalArgumentException("Company HashMap can contain only String, char, byte, int, long, float or double values");
                }
                if(! pCompany.containsKey("company_id") && ! pCompany.containsKey("name")) {
                    throw new IllegalArgumentException(
                            "Company HashMap must contain a String value with key \"company_id\" containing to the Company ID and a String value with key \"name\" containing the Company name"
                    );
                }
                this.company = pCompany;
            } else {
                this.company = null;
            }
            return this;
        }
        @Override public void start() {
            Customerly.registerUser(this.email, this.user_id, this.name, this.attributes, this.company, new CallbackWrapper(this.successCallback), new CallbackWrapper(this.failureCallback));
        }
    }

    public final class SetAttributesTask extends __Task {
        @NonNull private final HashMap<String,Object> attributes;
        /**
         * @param attributes The attributes of the user. Can contain only String, char, byte, int, long, float or double values
         * @throws IllegalArgumentException is thrown if the attributes check fails
         */
        private SetAttributesTask (@NonNull HashMap<String,Object> attributes) throws IllegalArgumentException {
            Collection<Object> attrs = attributes.values();
            for(Object attr : attrs) {
                if(     attr instanceof String ||
                        attr instanceof Integer ||
                        attr instanceof Byte ||
                        attr instanceof Long ||
                        attr instanceof Double ||
                        attr instanceof Float ||
                        attr instanceof Character ||
                        attr instanceof Boolean) {
                    continue;
                }
                throw new IllegalArgumentException("Attributes HashMap can contain only Strings, int, float, long, double or char values");
            }
            this.attributes = attributes;
        }
        @Override public void start() {
            Customerly.setAttributes(this.attributes, new CallbackWrapper(this.successCallback), new CallbackWrapper(this.failureCallback));
        }
    }

    /**
     * Utility builder for Company Map
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static class CompanyBuilder {
        @NonNull private final HashMap<String,Object> company = new HashMap<>();
        public CompanyBuilder(@NonNull String company_id, @NonNull String name) {
            super();
            this.company.put("company_id", company_id);
            this.company.put("name", name);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, @NonNull String value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, int value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, byte value) {
            return this.put(key, (Object)value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, long value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, double value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, float value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, char value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public CompanyBuilder put(@NonNull String key, boolean value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult private CompanyBuilder put(@NonNull String key, Object value) {
            if(!("company_id".equals(key) || "name".equals(key))) {
                this.company.put(key, value);
            }
            return this;
        }
        @NonNull public HashMap<String,Object> build() {
            return this.company;
        }
    }

    /**
     * Utility builder for Attributes Map
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static class AttributesBuilder {
        @NonNull private final HashMap<String,Object> attrs = new HashMap<>();
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, @NonNull String value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, int value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, byte value) {
            return this.put(key, (Object)value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, long value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, double value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, float value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, char value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult public AttributesBuilder put(@NonNull String key, boolean value) {
            return this.put(key, (Object) value);
        }
        @NonNull @CheckResult private AttributesBuilder put(@NonNull String key, Object value) {
            this.attrs.put(key, value);
            return this;
        }
        @NonNull public HashMap<String,Object> build() {
            return this.attrs;
        }
    }

    public final class SetCompanyTask extends __Task {
        @NonNull private final HashMap<String,Object> company;
        /**
         * @param pCompany The company of the user. The map must contain a String value with key "company_id" containing to the Company ID and a String value with key "name" containing the Company name. Can contain only String, char, int, long, float or double values.
         * @throws IllegalArgumentException is thrown if company map check fails
         */
        private SetCompanyTask(@NonNull HashMap<String,Object> pCompany) throws IllegalArgumentException {
            Collection<Object> attrs = pCompany.values();
            for(Object attr : attrs) {
                if(     attr instanceof String ||
                        attr instanceof Integer ||
                        attr instanceof Byte ||
                        attr instanceof Long ||
                        attr instanceof Double ||
                        attr instanceof Float ||
                        attr instanceof Character ||
                        attr instanceof Boolean) {
                    continue;
                }
                throw new IllegalArgumentException("Company HashMap can contain only String, char, byte, int, long, float or double values");
            }
            if(! pCompany.containsKey("company_id") && ! pCompany.containsKey("name")) {
                throw new IllegalArgumentException(
                        "Company HashMap must contain a String value with key \"company_id\" containing to the Company ID and a String value with key \"name\" containing the Company name"
                );
            }
            this.company = pCompany;
        }
        @Override public void start() {
            Customerly.setCompany(this.company, new CallbackWrapper(this.successCallback), new CallbackWrapper(this.failureCallback));
        }
    }

    /**
     * Call this method to build a task that force a check for pending Surveys or Message for the current user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @return The builded task that has to be started with his method {@link UpdateTask#start()}
     */
    @Deprecated
    @CheckResult @NonNull public UpdateTask update() {
        return new UpdateTask();
    }

    /**
     * Call this method to build a task that links your app user to the Customerly session.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param email The mail address of the user
     * @return The builded task that has to be started with his method {@link RegisterUserTask#start()}
     */
    @Deprecated
    @CheckResult @NonNull public RegisterUserTask registerUser(@NonNull String email) {
        return new RegisterUserTask(email);
    }

    /**
     * Call this method to build a task that add new custom attributes to the user.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param pAttributes Optional attributes for the user. Can contain only String, char, int, long, float or double values
     * @return The builded task that has to be started with his method {@link SetAttributesTask#start()}
     * @throws IllegalArgumentException is thrown if the attributes check fails
     */
    @Deprecated
    @CheckResult @NonNull public SetAttributesTask setAttributes(@NonNull HashMap<String, Object> pAttributes) throws IllegalArgumentException {
        return new SetAttributesTask(pAttributes);
    }

    /**
     * Call this method to build a task that add company attributes to the user.<br><br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param pCompany Optional company for the user. The map must contain a String value with key "company_id" containing to the Company ID and a String value with key "name" containing the Company name
     * @return The builded task that has to be started with his method {@link SetCompanyTask#start()}
     * @throws IllegalArgumentException is thrown if company map check fails
     */
    @Deprecated
    @CheckResult @NonNull public SetCompanyTask setCompany(@NonNull HashMap<String, Object> pCompany) throws IllegalArgumentException {
        return new SetCompanyTask(pCompany);
    }

    /**
     * Call this method to open the Support Activity.<br>
     * A call to this method will force the enabling if the support logic if it has been previously disabled with {@link #setSupportEnabled(boolean)}
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param activity The current activity
     */
    @Deprecated
    public void openSupport(@NonNull Activity activity) {
        Customerly.openSupport(activity);
    }

    /**
     * Call this method to close the user's Customerly session.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     */
    @Deprecated
    public void logoutUser() {
        Customerly.logoutUser();
    }

    /**
     * Call this method to keep track of custom labeled events.<br>
     * <br>
     * You have to configure the Customerly SDK before using this method with {@link #configure(Application,String)}
     * @param pEventName The event custom label
     */
    @Deprecated
    public void trackEvent(@NonNull final String pEventName) {
        Customerly.trackEvent(pEventName);
    }

    /**
     * Call this method to disable or enable the message receiving. It is ENABLED by default.<br>
     * A call to the method {@link #openSupport(Activity)} will force the enabling if it is disabled
     * @param enabled true if you want to enable it, false otherwise
     */
    @Deprecated
    public void setSupportEnabled(boolean enabled) {
        Customerly.setSupportEnabled(enabled);
    }

    /**
     * Call this method to disable or enable the survey receiving. It is ENABLED by default.<br>
     * @param enabled true if you want to enable it, false otherwise
     */
    @Deprecated
    public void setSurveysEnabled(boolean enabled) {
        Customerly.setSurveyEnabled(enabled);
    }

    /**
     * Call this method to specify an Activity that will never display a message popup or survey.<br>
     * Every Activity is ENABLED by default
     * @param activityClass The Activity class
     * @see #enableOn(Class)
     */
    @Deprecated
    public void disableOn(Class<? extends Activity> activityClass) {
        Customerly.disableOn(activityClass);
    }

    /**
     * Call this method to re-enable an Activity previously disabled with {@link #disableOn(Class)}.
     * @param activityClass The Activity class
     * @see #disableOn(Class)
     */
    @Deprecated
    public void enableOn(Class<? extends Activity> activityClass) {
        Customerly.enableOn(activityClass);
    }

    /**
     * @return Returns true if the SDK is available.
     */
    @Deprecated
    public boolean isSDKavailable() {
        return Customerly.isSdkAvailable();
    }
}
