package si.RSOteam8;



import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("images")
public class ImageResource {

    @GET
    public Response getAllImages() {
        List<Image> images = new LinkedList<Image>();
        Image image = new Image();
        image.setId("1");
        image.setImagename("https://en.wikipedia.org/wiki/Lenna#/media/File:Lenna_(test_image).png");
        images.add(image);
        return Response.ok(images).build();
    }

    @POST
    public Response addNewImage(Image image) {
        //Database.addCustomer(customer);
        return Response.noContent().build();
    }

    @DELETE
    @Path("{imageId}")
    public Response deleteImage(@PathParam("imageId") String imageId) {
        //Database.deleteCustomer(customerId);
        return Response.noContent().build();
    }
}
