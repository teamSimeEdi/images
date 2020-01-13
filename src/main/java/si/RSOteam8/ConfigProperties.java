package si.RSOteam8;

import com.kumuluz.ee.configuration.cdi.ConfigBundle;
import com.kumuluz.ee.configuration.cdi.ConfigValue;

import javax.enterprise.context.ApplicationScoped;


@ApplicationScoped
@ConfigBundle("config-properties")
public class ConfigProperties {

    @ConfigValue(value = "test", watch = true)
    private String test;
    @ConfigValue(value = "dburl", watch = true)
    private String dburl;
    @ConfigValue(value = "dbuser", watch = true)
    private String dbuser;
    @ConfigValue(value = "dbpass", watch = true)
    private String dbpass;
    @ConfigValue(value = "bucketname", watch = true)
    private String bucketname;


    public String getTest(){
        return test;
    }
    public void setTest(String test){
        this.test = test;
    }

    @ConfigValue(value = "health-demo", watch = true)
    private String healthdemo;

    public String getHealthdemo() {return healthdemo; }
    public void setHealthdemo(String hcd){this.healthdemo = hcd;}

    public String getDbpass() {
        return dbpass;
    }

    public void setDbpass(String dbpass) {
        this.dbpass = dbpass;
    }

    public String getDbuser() {
        return dbuser;
    }

    public void setDbuser(String dbuser) {
        this.dbuser = dbuser;
    }

    public String getDburl() {
        return dburl;
    }

    public void setDburl(String dburl) {
        this.dburl = dburl;
    }

    public String getBucketname() {
        return bucketname;
    }

    public void setBucketname(String bucketname) {
        this.bucketname = bucketname;
    }
}
