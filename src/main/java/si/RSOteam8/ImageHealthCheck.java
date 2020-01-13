package si.RSOteam8;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.logging.Logger;

@Readiness
@ApplicationScoped
public class ImageHealthCheck implements HealthCheck {

    @Inject
    private ConfigProperties cfg;

    public HealthCheckResponse call() {
        try {
            if (cfg.getHealthdemo().equals("true")){
                return HealthCheckResponse.up(ImageHealthCheck.class.getSimpleName());
            }
            else{
                return HealthCheckResponse.down(ImageHealthCheck.class.getSimpleName());

            }


        } catch (Exception exception){
            Logger.getLogger(ImageHealthCheck.class.getSimpleName()).warning(exception.getMessage()+" in ");
        }
        return HealthCheckResponse.down(ImageHealthCheck.class.getSimpleName());
    }

}
