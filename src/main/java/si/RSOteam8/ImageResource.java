package si.RSOteam8;


import com.google.cloud.storage.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import com.google.auth.oauth2.GoogleCredentials;

import com.kumuluz.ee.logs.LogManager;
import com.kumuluz.ee.logs.cdi.Log;
import com.kumuluz.ee.logs.cdi.LogParams;


import org.eclipse.microprofile.metrics.annotation.Counted;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.*;
import java.util.logging.Logger;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("images/{userId}")
@Log
public class ImageResource {
    

    @Inject
    private ConfigProperties cfg;

    @GET
    public Response getAllImages(@PathParam("userId") String username) {
        List<Image> images = new LinkedList<Image>();

        try (
                Connection conn = DriverManager.getConnection(cfg.getDburl(), cfg.getDbuser(), cfg.getDbpass());
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM images.images WHERE \"userid\" = " + "'"+username+"'");
        ) {
            while (rs.next()) {
                Image image = new Image();
                image.setId(rs.getString(1));
                image.setImagename(rs.getString(2));
                image.setUrl(rs.getString(3));
                images.add(image);
            }
        }
        catch (SQLException e) {
            System.err.println(e);
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        return Response.ok(images).build();
    }
    /*@Counted(name = "getAllImages-count")
    @GET
    public Response getAllImages() {
        Logger.getLogger(ImageHealthCheck.class.getSimpleName()).info("just testing");
        List<Image> images = new LinkedList<Image>();
        Image image = new Image();
        image.setId("1");
        image.setImagename(cfg.getTest());
        images.add(image);
        image = new Image();
        image.setId("2");
        image.setImagename("peterklepec");
        images.add(image);
        return Response.ok(images).build();
    }*/

    @GET
    @Path("{imageId}")
    public Response getImage(@PathParam("userId") String username,
                             @PathParam("imageId") String imagename) {

        try (
                Connection conn = DriverManager.getConnection(cfg.getDburl(), cfg.getDbuser(), cfg.getDbpass());
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM images.images WHERE \"userid\" = " + "'"+username+"' AND \"name\" = " + "'"+imagename+"'");
        ) {
            if (rs.next()){
                Image image = new Image();
                image.setId(rs.getString(1));
                image.setImagename(rs.getString(2));
                image.setUrl(rs.getString(3));
                return Response.ok(image).build();

            }
        }
        catch (SQLException e) {
            System.err.println(e);
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

   /* @POST
    public Response addNewImage(Image image) {
        //Database.addCustomer(customer);
        return Response.noContent().build();
    }*/
   @POST
   @Consumes(MediaType.MULTIPART_FORM_DATA)
   @Path("{imageId}")

   public Response addNewImage(@PathParam("userId") String username,
                               @PathParam("imageId") String imagename,
                               @Context HttpServletRequest request) {
         try (

                   Connection con = DriverManager.getConnection(cfg.getDburl(), cfg.getDbuser(), cfg.getDbpass());
                    Statement stmt = con.createStatement();


        ) {
             Part file = request.getPart("filename");
            String url = googleUpload(file);

            String imageName = new Scanner(request.getPart("name").getInputStream())
                    .useDelimiter("\\A")
                    .next();

                stmt.executeUpdate("INSERT INTO images.images (name, url, userid) VALUES ('"
                                + imageName + "', '" + url + "', '"+ username + "')",
                    Statement.RETURN_GENERATED_KEYS);



    }
        catch (Exception e) {
        System.err.println(e);
        return Response.status(Response.Status.FORBIDDEN).build();
    }

        return Response.noContent().build();
}
    @DELETE
    @Path("{imageId}")

    public Response deleteImage(@PathParam("userId") String username,
                                @PathParam("imageId") String imagename) {
        try (
                Connection conn = DriverManager.getConnection(cfg.getDburl(), cfg.getDbuser(), cfg.getDbpass());
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM images.images WHERE \"userid\" = " + "'"+username+"' AND \"name\" = " + "'"+imagename+"'");

        ) {
            if (rs.next()){
                String url = rs.getString(3);

                stmt.executeUpdate("DELETE FROM images.images WHERE \"userid\" = " + "'"+username+"' AND \"name\" = " + "'"+imagename+"'");

                googleDelete(url);
            }
        }
        catch (SQLException e) {
            System.err.println(e);
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        return Response.noContent().build();
    }
    /*@DELETE
    @Path("{imageId}")
    public Response deleteImage(@PathParam("imageId") String imageId) {
        //Database.deleteCustomer(customerId);
        return Response.noContent().build();
    }*/
    private static Storage storage = null;
     static {
        storage = StorageOptions.getDefaultInstance().getService();
    }
    private String googleUpload(Part filePart) throws IOException {

        DateTimeFormatter dtf = DateTimeFormat.forPattern("-YYYY-MM-dd-HHmmssSSS");
        DateTime dt = DateTime.now(DateTimeZone.UTC);
        String dtString = dt.toString(dtf);
        final String fileName = dtString+filePart.getSubmittedFileName() ;

        InputStream is = filePart.getInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] readBuf = new byte[4096];
        while (is.available() > 0) {
            int bytesRead = is.read(readBuf);
            os.write(readBuf, 0, bytesRead);
        }
        BlobInfo blobInfo =
                /*storage.create(
                        BlobInfo
                                .newBuilder(cfg.getBucketname(), fileName)
                                // Modify access list to allow all users with link to read file
                                .setAcl(new ArrayList<>(Arrays.asList(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))))
                                .build(),
                        os.toByteArray());*/
						
				Storage storage = StorageOptions
                .newBuilder()
                .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream("{  \"type\": \"service_account\",  \"project_id\": \"testuser-260815\",  \"private_key_id\": \"44c5e2acffcc732a33aab0235693a7e27d5be418\",  \"private_key\": \"-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDcimxbx56cneds\nn0hbWywg5YOTxI17KelCfwuYBy5eeYqfXACyc6kacv2vKs87TAy1iytvXToZWU39\nyq5aidsVib7nZf4hPj14TpJMLQWkxpWAc4NNeR/py4jmTYQYiJD9D0vGRExWM7Le\naCOYHvIXxyUIfpMguL1I5pq8cSul2g6Y5HGeQxiJu788LXNhj61+aiIxyTNhuAeZ\nBhOKt8zPE0zgbnLPZyj2N000n6uG0kZw/rAe62g1J6b4ENPc0JQ65ggWPZHMP9Ag\ngD6oVY4RtiBHLRCksXJu1qTtww1ECONdYvI7mv2vAYsETfGLE1EA0E7f0+EiWPNl\nR0GFmqW1AgMBAAECggEAEHmPvDB2lxUNrXXdqiNusbtrf52qgwWfvP2wIEiDDIAw\nXJS7IZmVy5nE/TGpvvCAr4wor75LYYrkEl4ydaWANgTu+6rJF/MmFOdrsuCXIBbM\nosaeS6qNeYHgpXGreUi0ag8bNkGyYSsmBMrLQ+kl5SRVjiOkeoiKbVh6vTKIE/DJ\naLbM3jRsjYAEEdN9e4KlLgz2577zQOF1THk3tGVP2y1PdhHmPzgoeKq0b0RTBsoQ\n4zq20gaeoUeHtv4Zuy1CZIXIgV0ut5WKvTbmajcDEbeNP4c3icxEkt/OvVuj56u7\nRGo/1h7vZAHUy15XTmP0kcke4CAbCBQTDYkY6jDX+QKBgQD3JvdUp7tt+M9gyYNW\nGKtVcdTB6DU0yorOSgL3mneCfO8D8py4oT4Y3XU6PiyZUfFq+7YecDwL4H9xgcNg\nPgrVyCLNBbDNoqqGFL4KznTkneKLOcvfGiDcA7fgcaKiOT4/eMmAi9Lq7x9Ea3g8\nEoS8fY+Sw3BSYvgBechElZocXQKBgQDkb5NGMZA90cXQdkT4Cn1tQXRYFHVxpqgV\nYDBKqr5SorZZuGzUaSYV3vEn+MakSVhbfQsSuN8lU52vi6w6pgc4hKmvey+euMQ3\nRf1WB6KoL31o45NPbKOF+pJWCgkb7H7L4ykZMTLjeBdBlGwZfXVkMm2oIt+2bR7e\nkRdnVUZZOQKBgQDBFqdKcxSikS1rP2QlYnaQ9bKzn3fZ+5dAHWB+EyblQjf1zJiW\nQhrikDniu+paCkPjQi4BT8wJt2W5xxhd23rZlFdj99sufLRetlvW039NkPAJt6jI\nb4BWg20no3/c3337VqGOlS6+Lv0tlgzEWe9r12jQz3G0W+/IL12//2T8XQKBgHXK\n2wR9FkXqX7io49OLhTXLZLTuh8j9CPtMyFoYRV/TK3iEUwoM1mi8t1nHPJcGgxta\nFVWoItjajxswSLVNW7fXILCuMtYDrJpb6tBry4IyStbFUvbHrGKv3LmlHyFZB+EW\n1+B3sCf/iu50HHHgcaIjJsxmfzlQd3SJGKpeHJJxAoGAPU7sfFirc70rpOH7wfrm\nKAtgdSLWEtl9uV3rwIM7zvQIh0Qym/JL5jETGrDGdIg/1/yJjFPbh16hZIal/Una\nPJkleDsNM4NVZsf4UuWMqMPSy40xxC1hSKthaPM+9N8jn5YSqfQOXxFi9xk5HkzU\nPE4PCoUneS0JGzeyUNf+sR0=\n-----END PRIVATE KEY-----\n\",  \"client_email\": \"46788361512-compute@developer.gserviceaccount.com\",  \"client_id\": \"101159909331207431481\",  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",  \"token_uri\": \"https://oauth2.googleapis.com/token\",  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/46788361512-compute%40developer.gserviceaccount.com\"}")))
                .setProjectId("testuser-260815")
                .build()
                .getService();
        // return the public download link
        return blobInfo.getMediaLink();
    }
    private void googleDelete(String url) {

             String blobName = url.substring(url.lastIndexOf('/') + 1);
             blobName = blobName.split("\\?")[0];
             BlobId blobId = BlobId.of(cfg.getBucketname(), blobName);
             storage.delete(blobId);

    }

}
