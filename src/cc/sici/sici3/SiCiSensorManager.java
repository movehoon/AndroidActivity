package cc.sici.sici3;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

public class SiCiSensorManager implements SensorEventListener {
    
    final int SENSOR_MAX = 13+1;
    
    private Handler mHandler = null;

    private SensorManager mSensorManager = null;
    
    public Sensor[] mSensors = new Sensor[SENSOR_MAX];
    public boolean[] mHasValue = new boolean[SENSOR_MAX];
    public float[][] mSensorValue = new float[SENSOR_MAX][];
    public float[] mMaxRange = new float[SENSOR_MAX];
    
    public SiCiSensorManager(SensorManager sensorManager, Handler handler)
    {
        mSensorManager = sensorManager;
        mHandler = handler;
        
        for (int i = 0 ; i < SENSOR_MAX ; i++) {
            mSensorValue[i] = new float[3];
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
            mSensors[Sensor.TYPE_ACCELEROMETER] = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null)
//            mSensors[Sensor.TYPE_AMBIENT_TEMPERATURE] = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null)
            mSensors[Sensor.TYPE_GRAVITY] = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null)
            mSensors[Sensor.TYPE_GYROSCOPE] = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null)
            mSensors[Sensor.TYPE_LIGHT] = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null)
            mSensors[Sensor.TYPE_LINEAR_ACCELERATION] = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
            mSensors[Sensor.TYPE_MAGNETIC_FIELD] = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null)
            mSensors[Sensor.TYPE_ORIENTATION] = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null)
            mSensors[Sensor.TYPE_PRESSURE] = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null)
            mSensors[Sensor.TYPE_PROXIMITY] = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
//        if (mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null)
//            mSensors[Sensor.TYPE_RELATIVE_HUMIDITY] = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);        
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null)
            mSensors[Sensor.TYPE_ROTATION_VECTOR] = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }
    
    public boolean enable(int type, boolean enable) {
        if (mSensors[type] != null) {
            if (enable)
                mSensorManager.registerListener(this, mSensors[type], SensorManager.SENSOR_DELAY_NORMAL);
            else
                mSensorManager.unregisterListener(this, mSensors[type]);
            mMaxRange[type] = mSensors[type].getMaximumRange();
            return true;
        }
        return false;
    }
    
    public boolean hasValue(int sensorType) {
        return mHasValue[sensorType];
    }
    
    public float getValue(int sensorType, int index) {
        mHasValue[sensorType] = false;
        return mSensorValue[sensorType][index];
    }
    
    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
        
    }

    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        mHasValue[type] = true;
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER :
            case Sensor.TYPE_GRAVITY :
            case Sensor.TYPE_GYROSCOPE :
            case Sensor.TYPE_LINEAR_ACCELERATION :
            case Sensor.TYPE_MAGNETIC_FIELD :
            case Sensor.TYPE_ORIENTATION :
            case Sensor.TYPE_ROTATION_VECTOR :
                mSensorValue[type][0] = event.values[0];
                mSensorValue[type][1] = event.values[1];
                mSensorValue[type][2] = event.values[2];
                break;

            case Sensor.TYPE_LIGHT :
            case Sensor.TYPE_PRESSURE :
            case Sensor.TYPE_PROXIMITY :
//            case Sensor.TYPE_RELATIVE_HUMIDITY :
//            case Sensor.TYPE_AMBIENT_TEMPERATURE :
                mSensorValue[type][0] = event.values[0];
                break;
        }
    }
}
