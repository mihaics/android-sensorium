/*
 *  This file is part of Sensorium.
 *
 *   Sensorium is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Sensorium is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with Sensorium. If not, see
 *   <http://www.gnu.org/licenses/>.
 * 
 * 
 */

package at.univie.sensorium.sensors;

import android.content.Context;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import at.univie.sensorium.SensorRegistry;
import at.univie.sensorium.privacy.Privacy;
import at.univie.sensorium.privacy.Privacy.PrivacyLevel;

public abstract class AbstractSensor {

    protected SensorValue timestamp;
    private boolean enabled = false;
    private List<SensorChangeListener> listeners;
    private String description = "";
    private Privacy.PrivacyLevel plevel;
    private String name = "Unnamed Sensor";


    public AbstractSensor() {
        timestamp = new SensorValue(SensorValue.UNIT.MILLISECONDS, SensorValue.TYPE.TIMESTAMP);
        this.listeners = new LinkedList<>();
        this.plevel = Privacy.PrivacyLevel.FULL;
    }

    public void enable() {
        if (!enabled) {
            try {
                _enable();

                SensorRegistry.getInstance().getPreferences().putBoolean(this.getClass().getName(), true);

                setPrivacylevel(PrivacyLevel.fromInt(SensorRegistry.getInstance().getPreferences().getInt(this.getClass().getName() + "-privacylevel", Privacy.PrivacyLevel.FULL.value())));

                enabled = true;
                notifyListeners();
            } catch (Exception e) {
                disable();
                Log.d(SensorRegistry.TAG, "Caught exception while enabling " + name + ": " + e.toString());
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Log.d(SensorRegistry.TAG, sw.toString());
            }
        }
    }

    protected abstract void _enable() throws SensorException;

    public void disable() {
        // if(enabled){
        try {
            SensorRegistry.getInstance().getPreferences().putBoolean(this.getClass().getName(), false);
            enabled = false;

            _disable();
            unsetallValues();
            notifyListeners();
        } catch (Exception e) {
            Log.d(SensorRegistry.TAG, "Caught exception while disabling " + name + ": " + e.toString());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Log.d(SensorRegistry.TAG, sw.toString());
        }
        // }
    }

    protected abstract void _disable();

    public void toggle() {
        if (enabled)
            disable();
        else
            enable();
    }

    protected void updateTimestamp() {
        timestamp.setValue(System.currentTimeMillis());
    }

    public void setState(boolean newState) {
        if (newState && !enabled)
            enable();
        else if (!newState && enabled)
            disable();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public List<SensorValue> getSensorValues() {
        List<SensorValue> values = new LinkedList<SensorValue>();

        values.add(timestamp); // as long as timestamp is the only sensorvalue
        // in the AbstractSensor superclass we should
        // add it manually

        Field[] fields = this.getClass().getDeclaredFields();

        try {
            for (Field f : fields) {
                f.setAccessible(true);
                Object o = f.get(this);
                if (o instanceof SensorValue) {
                    values.add((SensorValue) o);
                }

            }
        } catch (IllegalArgumentException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Log.d(SensorRegistry.TAG, sw.toString());
        } catch (IllegalAccessException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Log.d(SensorRegistry.TAG, sw.toString());
        }
        return values;
    }

    public void addListener(SensorChangeListener s) {
        this.listeners.add(s);
    }

    public void removeListener(SensorChangeListener s) {
        this.listeners.remove(s);
    }

    protected void notifyListeners() {
        updateTimestamp();
        for (SensorChangeListener l : listeners) {
            l.sensorUpdated(this);
        }

        StringBuilder sb = new StringBuilder();
        for (SensorValue val : getSensorValues()) {
            sb.append(val.getValue()).append(" ").append(val.getUnit().getName()).append("; ");
        }
        SensorRegistry.getInstance().log(this.getClass().getCanonicalName(), sb.toString());

        Log.d(SensorRegistry.TAG, sb.toString());
    }

    public Privacy.PrivacyLevel getPrivacylevel() {
        plevel = PrivacyLevel.fromInt(SensorRegistry.getInstance().getPreferences().getInt(this.getClass().getName() + "-privacylevel", plevel.value()));
        return plevel;
    }

    public void setPrivacylevel(Privacy.PrivacyLevel privacylevel) {
        SensorRegistry.getInstance().getPreferences().putInt(this.getClass().getName() + "-privacylevel", privacylevel.value());
        this.plevel = privacylevel;
        notifyListeners();
    }

    private void unsetallValues() {
        for (SensorValue s : getSensorValues()) {
            s.unsetValue();
        }
    }

    /**
     * Convenience method to access the application context. However, you should
     * not call this in your sensor constructor, the value might not be
     * initialized yet
     *
     * @return
     */
    protected Context getContext() {
        return SensorRegistry.getInstance().getContext();
    }

    public String getSensorStateDescription() {
        String sensorstate = "state unavailable";
        // SharedPreferences prefs =
        // PreferenceManager.getDefaultSharedPreferences(SensorRegistry.getInstance().getContext());
        // boolean enabled = prefs.getBoolean(this.getClass().getName(), true);
        if (!enabled)
            sensorstate = "Sensor disabled";
        else
            sensorstate = this.getPrivacylevel().toString();

        return sensorstate;
    }
}
