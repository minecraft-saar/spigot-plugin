import java.io.IOException;
import org.bukkit.Material;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Please enter a material");
            return;
        }
        int type = Integer.parseInt(args[0]);
        System.out.println(String.format("Material %d is %s", type, Material.values()[type]));
    }

}
