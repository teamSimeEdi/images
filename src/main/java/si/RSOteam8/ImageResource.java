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
                storage.create(
                        BlobInfo
                                .newBuilder(cfg.getBucketname(), fileName)
                                // Modify access list to allow all users with link to read file
                                .setAcl(new ArrayList<>(Arrays.asList(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))))
                                .build(),
                        os.toByteArray());
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
