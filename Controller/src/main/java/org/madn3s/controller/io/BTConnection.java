package org.madn3s.controller.io;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.UUID;

/**
 * Created by inaki on 12/7/13.
 */
public class BTConnection {

private static final String tag = "BTConnection";

    public final static String NXT_MAC_ADDRESS = "30:39:26:63:6F:36";

    private static BTConnection instance;
    private BluetoothAdapter localAdapter;
    private static Set<BluetoothDevice> pairedDevices;
    private static Set<BluetoothDevice> newDevices;
    private BluetoothSocket nxtSocket;
    private BluetoothSocket cam1Socket;
    private BluetoothSocket cam2Socket;
    boolean success=false;

    private BTConnection(){
        localAdapter=BluetoothAdapter.getDefaultAdapter();
        setPairedDevices(localAdapter.getBondedDevices());
        setNewDevices(localAdapter.getBondedDevices());
        cam1Socket = cam2Socket = null;
    }

    public static BTConnection getInstance(){
        if(instance == null) instance = new BTConnection();
        return instance;
    }

    public static Set<BluetoothDevice> getNewDevices() {
        return newDevices;
    }

    public static void setNewDevices(Set<BluetoothDevice> newDevices) {
        BTConnection.newDevices = newDevices;
    }

    private void enableBT(){
        if(!localAdapter.isEnabled()){
            localAdapter.enable();
        }
    }

    public void doDiscovery() {
        enableBT();
        if (localAdapter.isDiscovering()) localAdapter.cancelDiscovery();
        localAdapter.startDiscovery();
    }

    public void cancelDiscovery(){
        localAdapter.cancelDiscovery();
    }

    public boolean startMindstormConnection(){
        enableBT();
        Log.d(tag,"Conectando a "+NXT_MAC_ADDRESS);
        //get the BluetoothDevice of the NXT
        BluetoothDevice nxtBTDevice = localAdapter.getRemoteDevice(NXT_MAC_ADDRESS);

        //try to connect to the nxt
        try {
//            nxtSocket = localAdapter.listenUsingRfcommWithServiceRecord("MADN3S",UUID
//                    .fromString("00001101-0000-1000-8000-00805F9B34FB"));
//            nxtSocket.accept();

            nxtSocket = nxtBTDevice.createRfcommSocketToServiceRecord(UUID
                    .fromString("00001101-0000-1000-8000-00805F9B34FB"));

            if(null != nxtSocket) success = true;
        } catch (IOException e) {
            Log.d("Bluetooth","Err: Device not found or cannot connect");
            success=false;
        }finally{
            Log.d(tag, "isConnected() : " + String.valueOf(nxtSocket.isConnected()));
            Log.d(tag, "Direccion desde el socket: "+localAdapter.getRemoteDevice(NXT_MAC_ADDRESS).getAddress());
            Log.d(tag, "Clase desde el socket: "+localAdapter.getRemoteDevice(NXT_MAC_ADDRESS).getBluetoothClass());
        }

        return success;
    }


    public void writeMessage(byte msg) throws InterruptedException{
        if(nxtSocket!=null){
            try {
                OutputStreamWriter out = new OutputStreamWriter(nxtSocket.getOutputStream());
                out.write(msg);
                out.flush();


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int readMessage(String nxt){
        int n;
        if(nxtSocket!=null){
            try {
                InputStreamReader in=new InputStreamReader(nxtSocket.getInputStream());
                n=in.read();
                return n;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }
    
    public String readMessage(BluetoothSocket deviceSocket){
        BufferedReader buffer;
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        
        if(deviceSocket!=null){
            try {
                InputStreamReader in = new InputStreamReader(deviceSocket.getInputStream());
                buffer = new BufferedReader(in);
                
                while ((line = buffer.readLine()) != null) {
                    stringBuilder.append(line);
                }
                
                return stringBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean isConnected(){
        return nxtSocket.isConnected();
    }

    public Set<BluetoothDevice> getPairedDevices() {
        return pairedDevices;
    }

    public void setPairedDevices(Set<BluetoothDevice> pairedDevices) {
        this.pairedDevices = pairedDevices;
    }

    public BluetoothSocket getCam2Socket() {
        return cam2Socket;
    }

    public void setCam2Socket(BluetoothSocket cam2Socket) {
        this.cam2Socket = cam2Socket;
    }

    public BluetoothSocket getCam1Socket() {
        return cam1Socket;
    }

    public void setCam1Socket(BluetoothSocket cam1Socket) {
        this.cam1Socket = cam1Socket;
    }

    public BluetoothSocket getNxtSocket() {
        return nxtSocket;
    }

    public void setNxtSocket(BluetoothSocket nxtSocket) {
        this.nxtSocket = nxtSocket;
    }

    public void setCamSocket(BluetoothSocket camSocket) {
        if(cam1Socket == null){
            this.cam1Socket = camSocket;
        } else {
            this.cam2Socket = camSocket;
        }

    }
}
