package com.openxc.remote.sources.trace;

import java.io.File;
import java.io.IOException;

import java.lang.InterruptedException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import com.openxc.remote.sources.AbstractVehicleDataSourceCallback;
import com.openxc.remote.sources.VehicleDataSourceCallbackInterface;
import com.openxc.remote.sources.VehicleDataSourceException;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import com.openxc.R;

import android.test.suitebuilder.annotation.SmallTest;

public class TraceVehicleDataSourceTest extends AndroidTestCase {
    URI traceUri;
    URI malformedTraceUri;
    TraceVehicleDataSource source;
    Thread thread;
    VehicleDataSourceCallbackInterface callback;
    boolean receivedNumericalCallback;
    boolean receivedBooleanCallback;;
    double receivedNumber;
    boolean receivedBoolean;

    @Override
    protected void setUp() {
        try {
            traceUri = new URI("resource://" + R.raw.tracejson);
            malformedTraceUri = new URI("resource://" + R.raw.tracetxt);
        } catch(URISyntaxException e) {
            Assert.fail("Couldn't construct resource URIs: " + e);
        }

        callback = new AbstractVehicleDataSourceCallback() {
            public void receive(String name, Double value) {
                receivedNumericalCallback = true;
                receivedNumber = value;
            }

            public void receive(String name, Boolean value) {
                receivedBooleanCallback = true;
                receivedBoolean = value;
            }

            public void receive(String name, String value) { }
        };
    }

    @Override
    protected void tearDown() {
        if(source != null) {
            source.stop();
        }
        if(thread != null) {
            try {
                thread.join();
            } catch(InterruptedException e) {}
        }
    }

    private void startTrace(TraceVehicleDataSource source) {
        thread = new Thread(source);
        thread.start();
        try {
            Thread.sleep(300);
        } catch(InterruptedException e){ }
    }

    @SmallTest
    public void testPlaybackFile() throws InterruptedException,
            VehicleDataSourceException {
        receivedNumericalCallback = false;
        receivedBooleanCallback = false;
        source = new TraceVehicleDataSource(getContext(), callback, traceUri);
        startTrace(source);
        assertTrue(receivedNumericalCallback);
        assertTrue(receivedBooleanCallback);
        assertTrue(receivedNumber == 42.0 || receivedNumber == 94.1);
        assertEquals(receivedBoolean, false);
    }

    @SmallTest
    public void testPlaybackFromRegularFile() throws InterruptedException,
           VehicleDataSourceException {
        try {
            traceUri = new URI("file:///data/data/com.openxc/trace.json");
            malformedTraceUri = new URI("file:///data/data/com.openxc/malformed-trace.json");
        } catch(URISyntaxException e) {
            Assert.fail("Couldn't construct resource URIs: " + e);
        }

        try {
            FileUtils.copyInputStreamToFile(
                    getContext().getResources().openRawResource(
                        R.raw.tracejson), new File(traceUri));
            FileUtils.copyInputStreamToFile(
                    getContext().getResources().openRawResource(
                        R.raw.tracetxt), new File(malformedTraceUri));
        } catch(IOException e) {}
    }

    @SmallTest
    public void testMalformedJson() throws InterruptedException ,
            VehicleDataSourceException {
        receivedNumericalCallback = false;
        receivedBooleanCallback = false;
        source = new TraceVehicleDataSource(getContext(), callback,
                malformedTraceUri);
        startTrace(source);
        assertFalse(receivedNumericalCallback);
        source.stop();
    }

    @SmallTest
    public void testMissingFile() throws MalformedURLException,
            InterruptedException, VehicleDataSourceException,
            URISyntaxException {
        receivedNumericalCallback = false;
        receivedBooleanCallback = false;
        source = new TraceVehicleDataSource(getContext(), callback,
                new URL("file:///foo").toURI());
        startTrace(source);
        assertFalse(receivedNumericalCallback);
    }

    @SmallTest
    public void testConstructWithCallbackAndFile()
            throws VehicleDataSourceException {
        source = new TraceVehicleDataSource(getContext(), callback, traceUri);
    }
}
