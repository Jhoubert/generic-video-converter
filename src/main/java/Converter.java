

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Converter {
    public static void main(String[] args) {

        GenericVideoConverter videoConverter = new GenericVideoConverter();
        try{
            // videoConverter.convert("files/bun33s.flv", "files/bun33s.mp4")
            videoConverter.convertTo("files/bun33s.flv", "mp4");
            videoConverter.convertTo("files/bun33s.flv", "mkv");
            videoConverter.convertTo("files/invalidVideo.flv", "mp4");
        }catch (RuntimeException e){
            System.err.println("Error:" + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}