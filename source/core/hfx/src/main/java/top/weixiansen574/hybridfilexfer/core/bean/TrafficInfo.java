package top.weixiansen574.hybridfilexfer.core.bean;

public class TrafficInfo  {
    public String iName;
    public long uploadTraffic;
    public long downloadTraffic;

    public TrafficInfo(String iName, long uploadTraffic, long downloadTraffic) {
        this.iName = iName;
        this.uploadTraffic = uploadTraffic;
        this.downloadTraffic = downloadTraffic;
    }
    public TrafficInfo() {
    }
    public TrafficInfo(String iName) {
        this.iName = iName;
    }

}
