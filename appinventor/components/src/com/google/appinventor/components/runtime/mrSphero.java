// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;




import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.annotations.UsesNativeLibraries;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;







import com.google.appinventor.components.runtime.util.YailList;

import java.util.List;









import orbotix.macro.Delay;
import orbotix.macro.LoopEnd;
import orbotix.macro.LoopStart;
import orbotix.macro.MacroObject;
import orbotix.macro.RGB;
import orbotix.macro.RawMotor;
import orbotix.macro.Roll;
import orbotix.macro.RotateOverTime;
import orbotix.macro.Stabilization;
import orbotix.robot.base.*;
import orbotix.robot.sensor.AccelerometerData;
import orbotix.robot.sensor.AttitudeSensor;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.robot.sensor.LocatorData;
import orbotix.sphero.*;
import orbotix.view.connection.SpheroConnectionView;




/**
 * Multimedia component that plays sounds and optionally vibrates.  A
 * sound is specified via filename.  See also
 * {@link android.media.SoundPool}.
 *
 * @author sharon@google.com (Sharon Perl)
 */
@DesignerComponent(version = 1,
    description = "Control Sphero robotic ball (PLEASE NOTE THIS IS STILL IN DEVELOPMENT)",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "images/extension.png")
    //iconName = "images/mrSphero.png")
/**
* In INTERNAL catagory because still in development at the moment
*    category = ComponentCategory.USERINTERFACE)
**/

@SimpleObject
@UsesPermissions(permissionNames = "android.permission.BLUETOOTH_ADMIN, android.permission.BLUETOOTH")
@UsesLibraries(libraries = "RobotLibrary.jar" )
@UsesNativeLibraries(libraries = "libachievement_manager.so", 
                     v7aLibraries = "libachievement_manager.so")
public class mrSphero extends AndroidNonvisibleComponent 
    implements Component, OnDestroyListener{//, OnResumeListener, OnStopListener, OnDestroyListener, OnPauseListener, Deleteable {

  private YailList spherosFound = null;
  
  private Sphero mRobot = null;
  private String mspheroName = "";
  private boolean mAutoConnect = true;
  private int spheroHeading =0;
  private float spheroSpeed=0;
  private boolean motorFrozen = true;
  private boolean tailLightON = false;
  
  private int spheroColorRed = 0;
  private int spheroColorGreen = 0;
  private int spheroColorBlue = 0;
  private int spheroColorIntensity = 100;

  //sensor Feedback
  private int roll = 0 ;
  private int pitch=0;
  private int yaw =0;
   
  private float xDistanceTravelled=0;
  private float yDistanceTravelled=0;
  //private float xspeed=0;
  //private float yspeed=0;
  private float velocity=0;
  
  
  //Command constants
  private static final int CMD_RESET_HOME_POSITION = 1;
  private static final int CMD_ONCHARGER_KEEP_AWAKE = 2;
  private static final int CMD_ONCHARGER_DISCONNECT = 3;
  private static final int CMD_MOTOR_FREEZE = 4;
  private static final int CMD_MOTOR_UNFREEZE = 5;
  
  private static final int CMD_TAIL_LIGHT_ON = 6;
  private static final int CMD_TAIL_LIGHT_OFF = 7;
  private static final int CMD_COLOR = 8;
    
  private static final int CMD_DRIVE = 9;
  private static final int CMD_VIBRATE = 10;
  private static final int CMD_JUMP = 11;
  private static final int CMD_SPIN = 12;
  
  private static final int CONNECTIONSTATE_NOT_CONNECTED = 0;
  private static final int CONNECTIONSTATE_CONNECTED = 1;
  private static final int CONNECTIONSTATE_DISCOVERING = 2;
  private static final int CONNECTIONSTATE_DISCOVER_COMPLETE_NO_SPHERO_FOUND = 3;
  private static final int CONNECTIONSTATE_DISCOVER_COMPLETE_SPHERO_FOUND = 4;
  private static final int CONNECTIONSTATE_CONNECTION_FAILED = 5;
  private static final int CONNECTIONSTATE_BLUETOOTH_DISABLED = 6;
 
  
  private static final String CONNECTIONSTATEMSG_NOT_CONNECTED = "Sphero not Connected";
  private static final String CONNECTIONSTATEMSG_CONNECTED = "Sphero Connected ";
  private static final String CONNECTIONSTATEMSG_DISCOVERING = "Search for Sphero's in range ......";
  private static final String CONNECTIONSTATEMSG_DISCOVER_COMPLETE_NO_SPHERO_FOUND = "No Sphero have been found";
  private static final String CONNECTIONSTATEMSG_DISCOVER_COMPLETE_SPHERO_FOUND = "Sphero found";
  private static final String CONNECTIONSTATEMSG_BLUETOOTH_DISABLED = "Bluetooth is Disabled!";
 
  
  private int connectStatusCode = CMD_ONCHARGER_DISCONNECT;
  private String connectStatusMsg = CONNECTIONSTATEMSG_NOT_CONNECTED;
  
  
  
  //private boolean connected = false;
  private SpheroConnectionView mSpheroConnectionView;
  
  private final SensorListener mSensorListener = new SensorListener() {
    @Override
    public void sensorUpdated(DeviceSensorsData datum) {
      
      
      //Show attitude data
        AttitudeSensor attitude = datum.getAttitudeData();
        
        
        if (attitude != null) {
          roll = attitude.roll;
          pitch = attitude.pitch;
          yaw = attitude.yaw;
          
        }

        OnMovement(roll, pitch, yaw, velocity, xDistanceTravelled, yDistanceTravelled); 
        //OnMovement(roll, pitch, yaw, xDistanceTravelled, yDistanceTravelled, xspeed, yspeed);
        
    }
};


//Check location of Sphero
private LocatorListener mLocatorListener = new LocatorListener() {
  @Override
  public void onLocatorChanged(LocatorData locatorData) {
      if (locatorData != null) {
        xDistanceTravelled=locatorData.getPositionX();
        yDistanceTravelled=locatorData.getPositionY();
        float xspeed=locatorData.getVelocityX();
        float yspeed=locatorData.getVelocityY();
        velocity= (float) Math.sqrt((xspeed * xspeed)+ (yspeed * xspeed));
        OnMovement(roll, pitch, yaw, velocity, xDistanceTravelled, yDistanceTravelled);
        //xspeed=locatorData.getVelocityX();
        //yspeed=locatorData.getVelocityY();
        //OnMovement(roll, pitch, yaw, xDistanceTravelled, yDistanceTravelled, xspeed, yspeed);
      }
  }
};
  
private final CollisionListener mCollisionListener = new CollisionListener() {
  @Override
  public void collisionDetected(CollisionDetectedAsyncData collisionData) {
    int impactSpeed = (int) (collisionData.getImpactSpeed() * 100);
    int impactFrontBack = collisionData.getImpactPower().x;
    int impactSide = collisionData.getImpactPower().y;
    OnCollision(impactSpeed, impactFrontBack, impactSide);
  }
};

// Check for connection status to Sphero
private ConnectionListener mConnectionListener = new ConnectionListener() { 
  @Override
  public void onConnected(Robot robot) {
    mRobot = (Sphero) robot;
    mRobot.getSensorControl().setRate(10  /*Hz*/);
    mRobot.getSensorControl().addSensorListener(mSensorListener, SensorFlag.ACCELEROMETER_NORMALIZED, SensorFlag.ATTITUDE);
    mRobot.getSensorControl().addLocatorListener(mLocatorListener);
    mRobot.getCollisionControl().addCollisionListener(mCollisionListener);
    mRobot.getCollisionControl().startDetection(45, 45, 100, 100, 100);
    cmdResetHomePosition();
    mspheroName = mRobot.getName();
    connectStatusCode=CONNECTIONSTATE_CONNECTED;
    connectStatusMsg=CONNECTIONSTATEMSG_CONNECTED;
    Connects(mspheroName);
    //OnConnectStateChanged(mspheroName,CONNECTIONSTATE_CONNECTED,CONNECTIONSTATEMSG_CONNECTED);
  }

  @Override
  public void onConnectionFailed(Robot sphero) {
   // OnConnectStateChanged(mspheroName,CONNECTIONSTATE_CONNECTION_FAILED);
  }

  @Override
  public void onDisconnected(Robot sphero) {
     mRobot = null;
     mspheroName = "";
     connectStatusCode=CONNECTIONSTATE_NOT_CONNECTED;
     connectStatusMsg=CONNECTIONSTATEMSG_NOT_CONNECTED;
     Disconnects(); 
     //OnConnectStateChanged(mspheroName,CONNECTIONSTATE_NOT_CONNECTED,CONNECTIONSTATEMSG_NOT_CONNECTED);
  }
  
  
};

private DiscoveryListener mDiscoveryListener = new DiscoveryListener() {
  @Override
  public void onBluetoothDisabled() {
    connectStatusCode=CONNECTIONSTATE_BLUETOOTH_DISABLED;
    connectStatusMsg=CONNECTIONSTATEMSG_BLUETOOTH_DISABLED;
    //OnConnectStateChanged(mspheroName,CONNECTIONSTATE_BLUETOOTH_DISABLED,CONNECTIONSTATEMSG_BLUETOOTH_DISABLED);
  }

  @Override
  public void discoveryComplete(List<Sphero> spheros) {
    
    if(spheros.size()>0) {
      connectStatusCode=CONNECTIONSTATE_DISCOVER_COMPLETE_SPHERO_FOUND;
      connectStatusMsg=CONNECTIONSTATEMSG_DISCOVER_COMPLETE_SPHERO_FOUND;
      
      //OnConnectStateChanged("",CONNECTIONSTATE_DISCOVER_COMPLETE_SPHERO_FOUND,CONNECTIONSTATEMSG_DISCOVER_COMPLETE_SPHERO_FOUND);
    } else {
      connectStatusCode=CONNECTIONSTATE_DISCOVER_COMPLETE_NO_SPHERO_FOUND;
      connectStatusMsg=CONNECTIONSTATEMSG_DISCOVER_COMPLETE_NO_SPHERO_FOUND;
      
      //OnConnectStateChanged("",CONNECTIONSTATE_DISCOVER_COMPLETE_NO_SPHERO_FOUND,CONNECTIONSTATEMSG_DISCOVER_COMPLETE_NO_SPHERO_FOUND);  
    }
    
  }

  @Override
  public void onFound(List<Sphero> sphero) {
//    spherosFound=(YailList) sphero.;
    if (mAutoConnect) {
      RobotProvider.getDefaultProvider().connect(sphero.iterator().next());
      
      //OnConnectStateChanged(mspheroName,CONNECTIONSTATE_CONNECTED,CONNECTIONSTATEMSG_CONNECTED);
    }  
  }
};


  public mrSphero(ComponentContainer container) {
    super(container.$form());
     mSpheroConnectionView = new SpheroConnectionView(container.$context()) ;
     mSpheroConnectionView.addConnectionListener(mConnectionListener);
     mSpheroConnectionView.addDiscoveryListener(mDiscoveryListener);
     ConnectToSphero();
  //   container.$add(this);
  }
  
  
  

  /*-------------------------------------------
   *Connection setup view 
   */
    
 
       
  
 // @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
 //     defaultValue = "True")
 // @SimpleProperty(userVisible = false)
 // public void AutoConnect(boolean connect) {
 //   mAutoConnect = connect;
 // }
 
 // @SimpleProperty()
 // public YailList SpherosFound() {
 //   return spherosFound;
 // }
  
  
 // @SimpleProperty()
 // public boolean isAutoConnectEnabled() {
 //   return mAutoConnect;
 // }
 // @SimpleFunction
 // public void DiscoverySpheros(boolean autoConnect) {
 //   if (mRobot == null) { 
 //     OnConnectStateChanged(mspheroName,CONNECTIONSTATE_DISCOVERING,CONNECTIONSTATEMSG_DISCOVERING);
 //     mAutoConnect=autoConnect;
 //     mSpheroConnectionView.startDiscovery();
 //   }
 // }
  
  @SimpleFunction
  public void  ConnectToSphero() {
    if (mRobot == null) { 
      connectStatusCode=CONNECTIONSTATE_DISCOVERING;
      connectStatusMsg=CONNECTIONSTATEMSG_DISCOVERING;
      //OnConnectStateChanged(mspheroName,CONNECTIONSTATE_DISCOVERING,CONNECTIONSTATEMSG_DISCOVERING);
      mAutoConnect=true;
      mSpheroConnectionView.startDiscovery();
    }
  }
   
  //@SimpleFunction
  //public void DiscoverySpherosAutoConnect() {
  //  OnConnectStateChanged(mspheroName,3);
  //  autoConnect=true;
  //  mSpheroConnectionView.startDiscovery();
  //}
   
   @SimpleFunction
   public void DisConnect() {
     if (mRobot != null) {
       mRobot.disconnect();
     }
   }
   
   //@SimpleEvent
   //public void OnConnectStateChanged(String spheroName,int StateCode, String StateMessage) {
   //  EventDispatcher.dispatchEvent(this, "OnConnectStateChanged",spheroName, StateCode, StateMessage);
   //}
   
   @SimpleEvent
   public void Connects(String spheroName) {
     EventDispatcher.dispatchEvent(this, "Connects",spheroName);
   }
   
   @SimpleEvent
   public void Disconnects() {
     EventDispatcher.dispatchEvent(this, "Disconnects");
   }
   
  
   @SimpleProperty
   public int ConnectStatusCode() {
     return connectStatusCode;
   }
   
   @SimpleProperty
   public String ConnectStatusMessage() {
     return connectStatusMsg;
   }
   
   
   
   
   
   /*-------------------------------------------
    *Information blocks
    */
   @SimpleProperty
   public String SpheroName() {
     return mspheroName;
   }
   
   @SimpleProperty
   public boolean IsTailLightOn() {
     return tailLightON;
   }
     
   @SimpleProperty
   public boolean IsMotorLocked() {
     return !motorFrozen;
   }
   
   
   
   /*-------------------------------------------
    *Sensor Feedback events
    */
   
   @SimpleEvent
   public void OnCollision(int impactSpeed, int impactStengthFB, int impactStrengthLR) {
     EventDispatcher.dispatchEvent(this, "OnCollision", impactSpeed, impactStengthFB, impactStrengthLR);
   }


   @SimpleEvent
   public void OnMovement(int tiltFB, int tiltLR, int heading, float velocity, float distanceTravelledInY, float distanceTravelledInX) {
     EventDispatcher.dispatchEvent(this, "OnMovement", tiltFB, tiltLR, heading, velocity, distanceTravelledInX, distanceTravelledInY);
   }
   
   @SimpleProperty
   public float SensorTiltFB() {
    return pitch; 
   }
   
   @SimpleProperty
   public float SensorTiltLR() {
    return roll; 
   }
   
   @SimpleProperty
   public float SensorHeading() {
    return spheroHeading; 
   }
   
   
   @SimpleProperty
   public float DistanceTravelledInX() {
    return xDistanceTravelled; 
   }
   
   @SimpleProperty
   public float DistanceTravelledInY() {
    return yDistanceTravelled; 
   }
   
   @SimpleProperty
   public float SensorVelocity() {
     return velocity; 
   }
   
  
  
  
  
  
  
   /*-------------------------------------------
    *Macro Blocks
    */
     
   @SimpleFunction
   public void CommandStop() {
     AbortMacroCommand.sendCommand(mRobot); // abort command
     mRobot.enableStabilization(motorFrozen);// turn on stabilization
     mRobot.stop();
   }

   
   @SimpleFunction
   public void CommandExecute(String cmd) {
     String cmdStr[] = cmd.split(",");
     int cmdCode = Integer.parseInt(cmdStr[0]);
     
     MacroObject macro = new MacroObject();
     
     
     switch (cmdCode) {
       case CMD_RESET_HOME_POSITION:
         mRobot.startCalibration();
         mRobot.stopCalibration(true);
         spheroHeading=0;
         ConfigureLocatorCommand.sendCommand(mRobot, 0, 0, 0, 0);
         if (tailLightON) {
           mRobot.setBackLEDBrightness(1f);
         } else {
           mRobot.setBackLEDBrightness(0);
         }
         mRobot.enableStabilization(motorFrozen); 
         break;
            
       case CMD_ONCHARGER_KEEP_AWAKE:
         mRobot.getConfiguration().setPersistentFlag(PersistentOptionFlags.PreventSleepInCharger, true);
         break;
       
       case CMD_ONCHARGER_DISCONNECT:
         mRobot.getConfiguration().setPersistentFlag(PersistentOptionFlags.PreventSleepInCharger, true);
         break;
       
       case CMD_MOTOR_FREEZE:
       //  motorFrozen=true;
         mRobot.enableStabilization(motorFrozen); 
         break;
       
       case CMD_MOTOR_UNFREEZE:
       //  motorFrozen=false;
         mRobot.enableStabilization(motorFrozen); 
         break;
       
       case CMD_TAIL_LIGHT_ON:
         tailLightON = true;
         mRobot.setBackLEDBrightness(1f);
         break;
       
       case CMD_TAIL_LIGHT_OFF:
         tailLightON = false;
         mRobot.setBackLEDBrightness(0);
         break;
       
       case CMD_COLOR:
         int intensity = (int) 255-((255/100)*spheroColorIntensity);
         int red = spheroColorRed - intensity;
         red = red<0?0:red;
         int green = spheroColorGreen - intensity;
         green = green<0?0:green;
         int blue = spheroColorBlue - intensity;
         blue = blue<0?0:blue;
         mRobot.setColor(red,green,blue);
         break;
       
       case CMD_DRIVE:
         //if heading is not between 0 - 359 recalculate so it's within these limits
         spheroHeading = (int) (spheroHeading - (Math.floor(spheroHeading / 360) * 360));
         spheroHeading = spheroHeading < 0 ? 360 + spheroHeading : spheroHeading;
         float speed = spheroSpeed/100;
         mRobot.drive((float)spheroHeading, speed);
         break;
       
       case CMD_VIBRATE:
         int milliseconds=Integer.parseInt(cmdStr[1]);
         int strength=Integer.parseInt(cmdStr[2]);
         // You must turn stabilization off to use the raw motors
         macro.addCommand(new Stabilization(false,0));
         macro.addCommand(new LoopStart(milliseconds));
         // Run both motors forward for a milli second
         macro.addCommand(new RawMotor(RawMotor.DriveMode.FORWARD, 90, RawMotor.DriveMode.FORWARD, 90, 0));
         macro.addCommand(new Delay(strength));
         // Run both motors backward for a milli second (to simulate a vibration)
         macro.addCommand(new RawMotor(RawMotor.DriveMode.REVERSE, 90, RawMotor.DriveMode.REVERSE, 90, 0));
         macro.addCommand(new Delay(strength));
         macro.addCommand(new LoopEnd());
         // Remember to turn stabilization back on to avoid difficulties driving
         macro.addCommand(new Stabilization(motorFrozen,0));
         mRobot.executeMacro(macro);
         break;
         
       case CMD_JUMP:
         int direction=Integer.parseInt(cmdStr[1]);
         int noJumps=Integer.parseInt(cmdStr[2]);
         // You must turn stabilization off to use the raw motors
         macro.addCommand(new Stabilization(false,0));
         // Run both motors forward at full power
         if (direction==1) {
           //jump forward
           macro.addCommand(new RawMotor(RawMotor.DriveMode.FORWARD, 255, RawMotor.DriveMode.FORWARD, 255, 0));
         } else {
           //jump back
           macro.addCommand(new RawMotor(RawMotor.DriveMode.REVERSE, 255, RawMotor.DriveMode.REVERSE, 255, 0));
         }
         // Delay for a certain time period (150 is a time to do a full flip)
         macro.addCommand(new Delay(noJumps*150));
         // Remember to turn stabilization back on
         macro.addCommand(new Stabilization(motorFrozen,0));
         mRobot.executeMacro(macro);
         break;
         
         
       case CMD_SPIN:  
        
         int delayValue = Integer.parseInt(cmdStr[1]); //speed
         int mDirection = delayValue<0?-360:360;
         delayValue = delayValue<0?-delayValue:delayValue;
         
         int times = Integer.parseInt(cmdStr[2]);
         times = times<0?-times:times;
         macro.addCommand(new Stabilization(true,0));
         macro.addCommand(new LoopStart(times));
           macro.addCommand(new RotateOverTime(mDirection, delayValue));
           macro.addCommand(new Delay(delayValue));
         macro.addCommand(new LoopEnd());
         macro.addCommand(new Stabilization(motorFrozen,0));
        // macro.setMode(MacroObject.MacroObjectMode.Normal);
         mRobot.executeMacro(macro);
         break;
         
            
       default: 
              
     }
   }
   
   
   
   
   
   
   /*-------------------------------------------
    *Macro command Blocks
    */
   @SimpleFunction
   public String cmdResetHomePosition() {
     return Integer.toString(CMD_RESET_HOME_POSITION);
   }
   
   @SimpleFunction
   public String cmdOnChargerKeepAwake() {
     return Integer.toString(CMD_ONCHARGER_KEEP_AWAKE);
   }
   
   @SimpleFunction
   public String cmdOnChargerDisconnect() {
     return Integer.toString(CMD_ONCHARGER_DISCONNECT);
   }
   
   @SimpleFunction
   public String cmdMotorLock() {
     motorFrozen=false;
     return Integer.toString(CMD_MOTOR_FREEZE);
   }
   
   @SimpleFunction
   public String cmdMotorUnlock() {
     motorFrozen=true;
     return Integer.toString(CMD_MOTOR_UNFREEZE);
   }
   
   @SimpleFunction
   public String cmdTailLightOn() {
     return Integer.toString(CMD_TAIL_LIGHT_ON);
   }
   
   @SimpleFunction
   public String cmdTailLightOff() {
     return Integer.toString(CMD_TAIL_LIGHT_OFF);
   }
   
   @SimpleFunction
   public String cmdSetColor(int color) {
      int red = (color>>16) & 0x0ff;
      int green=(color>>8) & 0x0ff;
      int blue= (color) & 0x0ff;
      spheroColorRed = red;
      spheroColorGreen = green;
      spheroColorBlue = blue;
      
      return Integer.toString(CMD_COLOR);
   }
   
   
   @SimpleFunction(userVisible = false)
   public String cmdColor(int red, int green, int blue) {
      // int red = (int)((Math.pow(256,3)+color) / 65536); //where rgbs is an array of integers, every single integer represents the RGB values combined in some way
      // int green = (int) (((Math.pow(256,3)+color) / 256 ) % 256 );
      // int blue = (int) ((Math.pow(256,3)+color)%256);
     red=red>255?255:red;
     red=red<0?0:red;
     green=green>255?255:green;
     green=green<0?0:green;
     blue=blue>255?255:blue;
     blue=blue<0?0:blue;
     
     spheroColorRed = red;
     spheroColorGreen = green;
     spheroColorBlue = blue;
     
     return Integer.toString(CMD_COLOR);
   }

   @SimpleFunction
   public String cmdColorBrightness(int intensity) {
     intensity=intensity>100?100:intensity;
     intensity=intensity<0?0:intensity;
     spheroColorIntensity = intensity;
     // int red = (int)((Math.pow(256,3)+color) / 65536); //where rgbs is an array of integers, every single integer represents the RGB values combined in some way
      // int green = (int) (((Math.pow(256,3)+color) / 256 ) % 256 );
      // int blue = (int) ((Math.pow(256,3)+color)%256);
     return Integer.toString(CMD_COLOR);
            
   }

   //CMD_COLOR_BRIGHTNESS:
   
   @SimpleFunction
   public String cmdSetHeading(int degrees) {
     spheroHeading = degrees;
     return Integer.toString(CMD_DRIVE);
   }
   
   @SimpleFunction
   public String cmdSetSpeed(float speed) {
     spheroSpeed=speed>100?100:speed;
     spheroSpeed=speed<0?0:speed;
     return Integer.toString(CMD_DRIVE);
   }
  
   
   @SimpleFunction
   public String cmdVibrate(int milliseconds, int strength) {
     return Integer.toString(CMD_VIBRATE) + "," +
         Integer.toString(milliseconds) + "," +
         Integer.toString(strength);
   }
   
   @SimpleFunction
   public String cmdJumpForward(int times) {
     times=times<0?1:times;
     int DIRECTION=1;
     return Integer.toString(CMD_JUMP) + "," +
            Integer.toString(DIRECTION) + "," +
            Integer.toString(times);
   }

   @SimpleFunction
   public String cmdJumpBack(int times) {
     times=times<0?1:times;
     int DIRECTION=0;
     return Integer.toString(CMD_JUMP) + "," +
            Integer.toString(DIRECTION) + "," +
            Integer.toString(times);
   }

  
   @SimpleFunction(userVisible = false)
   public String cmdRotateBy(int degrees) {
     return cmdSetHeading(spheroHeading + degrees);
   } 
     
   @SimpleFunction
   public String cmdSpin(int speed, int times) {
      return Integer.toString(CMD_SPIN) + "," +
             Integer.toString(speed) + "," +
             Integer.toString(times);
   }




  @Override
  public void onDestroy() {
    //when app closed disconnect sphero
    DisConnect();
  }
   
   
}