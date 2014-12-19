package com.microsoft.applicationinsights.channel.contracts;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.ArrayList;
import com.microsoft.applicationinsights.channel.contracts.shared.ITelemetry;
import com.microsoft.applicationinsights.channel.contracts.shared.ITelemetryData;
import com.microsoft.applicationinsights.channel.contracts.shared.IContext;
import com.microsoft.applicationinsights.channel.contracts.shared.IJsonSerializable;
import com.microsoft.applicationinsights.channel.contracts.shared.JsonHelper;

/**
 * Data contract class Envelope.
 */
public class Envelope implements
    IJsonSerializable
{
    /**
     * Backing field for property Ver.
     */
    private int ver = 1;
    
    /**
     * Backing field for property Name.
     */
    private String name;
    
    /**
     * Backing field for property Time.
     */
    private String time;
    
    /**
     * Backing field for property SampleRate.
     */
    private double sampleRate = 100.0;
    
    /**
     * Backing field for property Seq.
     */
    private String seq;
    
    /**
     * Backing field for property IKey.
     */
    private String iKey;
    
    /**
     * Backing field for property Flags.
     */
    private long flags;
    
    /**
     * Backing field for property DeviceId.
     */
    private String deviceId;
    
    /**
     * Backing field for property Os.
     */
    private String os;
    
    /**
     * Backing field for property OsVer.
     */
    private String osVer;
    
    /**
     * Backing field for property AppId.
     */
    private String appId;
    
    /**
     * Backing field for property AppVer.
     */
    private String appVer;
    
    /**
     * Backing field for property UserId.
     */
    private String userId;
    
    /**
     * Backing field for property Tags.
     */
    private HashMap<String, String> tags;
    
    /**
     * Backing field for property Data.
     */
    private Base data;
    
    /**
     * Initializes a new instance of the <see cref="Envelope"/> class.
     */
    public Envelope()
    {
        this.InitializeFields();
    }
    
    /**
     * Gets the Ver property.
     */
    public int getVer() {
        return this.ver;
    }
    
    /**
     * Sets the Ver property.
     */
    public void setVer(int value) {
        this.ver = value;
    }
    
    /**
     * Gets the Name property.
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Sets the Name property.
     */
    public void setName(String value) {
        this.name = value;
    }
    
    /**
     * Gets the Time property.
     */
    public String getTime() {
        return this.time;
    }
    
    /**
     * Sets the Time property.
     */
    public void setTime(String value) {
        this.time = value;
    }
    
    /**
     * Gets the SampleRate property.
     */
    public double getSampleRate() {
        return this.sampleRate;
    }
    
    /**
     * Sets the SampleRate property.
     */
    public void setSampleRate(double value) {
        this.sampleRate = value;
    }
    
    /**
     * Gets the Seq property.
     */
    public String getSeq() {
        return this.seq;
    }
    
    /**
     * Sets the Seq property.
     */
    public void setSeq(String value) {
        this.seq = value;
    }
    
    /**
     * Gets the IKey property.
     */
    public String getIKey() {
        return this.iKey;
    }
    
    /**
     * Sets the IKey property.
     */
    public void setIKey(String value) {
        this.iKey = value;
    }
    
    /**
     * Gets the Flags property.
     */
    public long getFlags() {
        return this.flags;
    }
    
    /**
     * Sets the Flags property.
     */
    public void setFlags(long value) {
        this.flags = value;
    }
    
    /**
     * Gets the DeviceId property.
     */
    public String getDeviceId() {
        return this.deviceId;
    }
    
    /**
     * Sets the DeviceId property.
     */
    public void setDeviceId(String value) {
        this.deviceId = value;
    }
    
    /**
     * Gets the Os property.
     */
    public String getOs() {
        return this.os;
    }
    
    /**
     * Sets the Os property.
     */
    public void setOs(String value) {
        this.os = value;
    }
    
    /**
     * Gets the OsVer property.
     */
    public String getOsVer() {
        return this.osVer;
    }
    
    /**
     * Sets the OsVer property.
     */
    public void setOsVer(String value) {
        this.osVer = value;
    }
    
    /**
     * Gets the AppId property.
     */
    public String getAppId() {
        return this.appId;
    }
    
    /**
     * Sets the AppId property.
     */
    public void setAppId(String value) {
        this.appId = value;
    }
    
    /**
     * Gets the AppVer property.
     */
    public String getAppVer() {
        return this.appVer;
    }
    
    /**
     * Sets the AppVer property.
     */
    public void setAppVer(String value) {
        this.appVer = value;
    }
    
    /**
     * Gets the UserId property.
     */
    public String getUserId() {
        return this.userId;
    }
    
    /**
     * Sets the UserId property.
     */
    public void setUserId(String value) {
        this.userId = value;
    }
    
    /**
     * Gets the Tags property.
     */
    public HashMap<String, String> getTags() {
        if (this.tags == null) {
            this.tags = new HashMap<String, String>();
        }
        return this.tags;
    }
    
    /**
     * Sets the Tags property.
     */
    public void setTags(HashMap<String, String> value) {
        this.tags = value;
    }
    
    /**
     * Gets the Data property.
     */
    public Base getData() {
        return this.data;
    }
    
    /**
     * Sets the Data property.
     */
    public void setData(Base value) {
        this.data = value;
    }
    

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    @Override
    public void serialize(Writer writer) throws IOException
    {
        if (writer == null)
        {
            throw new IllegalArgumentException("writer");
        }
        
        writer.write('{');
        this.serializeContent(writer);
        writer.write('}');
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    protected String serializeContent(Writer writer) throws IOException
    {
        String prefix = "";
        if (!(this.ver == 0))
        {
            writer.write(prefix + "\"ver\":");
            writer.write(JsonHelper.convert(this.ver));
            prefix = ",";
        }
        
        writer.write(prefix + "\"name\":");
        writer.write(JsonHelper.convert(this.name));
        prefix = ",";
        
        writer.write(prefix + "\"time\":");
        writer.write(JsonHelper.convert(this.time));
        prefix = ",";
        
        if (!(this.sampleRate == 0.0d))
        {
            writer.write(prefix + "\"sampleRate\":");
            writer.write(JsonHelper.convert(this.sampleRate));
            prefix = ",";
        }
        
        if (!(this.seq == null))
        {
            writer.write(prefix + "\"seq\":");
            writer.write(JsonHelper.convert(this.seq));
            prefix = ",";
        }
        
        if (!(this.iKey == null))
        {
            writer.write(prefix + "\"iKey\":");
            writer.write(JsonHelper.convert(this.iKey));
            prefix = ",";
        }
        
        if (!(this.flags == 0L))
        {
            writer.write(prefix + "\"flags\":");
            writer.write(JsonHelper.convert(this.flags));
            prefix = ",";
        }
        
        if (!(this.deviceId == null))
        {
            writer.write(prefix + "\"deviceId\":");
            writer.write(JsonHelper.convert(this.deviceId));
            prefix = ",";
        }
        
        if (!(this.os == null))
        {
            writer.write(prefix + "\"os\":");
            writer.write(JsonHelper.convert(this.os));
            prefix = ",";
        }
        
        if (!(this.osVer == null))
        {
            writer.write(prefix + "\"osVer\":");
            writer.write(JsonHelper.convert(this.osVer));
            prefix = ",";
        }
        
        if (!(this.appId == null))
        {
            writer.write(prefix + "\"appId\":");
            writer.write(JsonHelper.convert(this.appId));
            prefix = ",";
        }
        
        if (!(this.appVer == null))
        {
            writer.write(prefix + "\"appVer\":");
            writer.write(JsonHelper.convert(this.appVer));
            prefix = ",";
        }
        
        if (!(this.userId == null))
        {
            writer.write(prefix + "\"userId\":");
            writer.write(JsonHelper.convert(this.userId));
            prefix = ",";
        }
        
        if (!(this.tags == null))
        {
            writer.write(prefix + "\"tags\":");
            JsonHelper.writeDictionary(writer, this.tags);
            prefix = ",";
        }
        
        if (!(this.data == null))
        {
            writer.write(prefix + "\"data\":");
            JsonHelper.writeJsonSerializable(writer, this.data);
            prefix = ",";
        }
        
        return prefix;
    }
    
    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
        
    }
}
