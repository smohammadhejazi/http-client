import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class represents a console. It is used to get command line arguments
 * and make request.
 */
public class Console
{
    //Command line arguments that are allowed
    private static ArrayList<String> COMMANDS = new ArrayList<String>(
            Arrays.asList("-M", "--method", "-H", "--headers", "-i", "-h",
                    "--help", "-O", "--output", "-S", "--save", "-d",
                    "--data"));
    //URL of the request
    private URL url;
    //Arguments without the url
    private ArrayList<String> options;
    //The currently sent request
    private Request request = null;

    /**
     * Create a new Console. It starts the whole process.
     * @param args unused
     */
    public Console(String[] args)
    {
        ArrayList<String> argsArray = new ArrayList<>(Arrays.asList(args));
        options = new ArrayList<>();

        if (!checkArguments(argsArray))
        {
            System.err.println("Error: Malformed arguments");
            return;
        }

        if (args[0].equals("list"))
        {
            listRequestHistory();
        }
        else if (args[0].equals("fire"))
        {
            try
            {
                loadRequest(Integer.parseInt(args[1]) - 1, 1);
            }
            catch (NumberFormatException e)
            {
                System.err.println("Error: Number is not valid");
            }

        }
        else if (args[0].equals("-h") || args[0].equals("--help"))
        {
            printHelp();
        }
        else
        {
            if (separateArguments(argsArray))
                request = new Request(url, options);
        }
    }

    /**
     * This is the main method for running the Console.
     * @param args unused
     */
    public static void main(String[] args)
    {
        Console console = new Console(args);
    }

    /**
     * Separates the url and option arguments.
     * @param argsArray Array of the command line args
     * @return If the url is not malformed returns false, and true otherwise
     */
    private boolean separateArguments(ArrayList<String> argsArray)
    {
        for (int i = 0; i < argsArray.size(); i++)
        {
            String argument = argsArray.get(i);
            if (argument.charAt(0) == '-')
            {
                options.add(argument);
                if (argument.equals("-i") || argument.equals("-S") ||
                        argument.equals("--save"))
                {
                    continue;
                }
                i++;
                if (i < argsArray.size())
                    options.add(argsArray.get(i));
            }
            else
            {
                try
                {
                    url = new URL(argument);
                }
                catch (MalformedURLException e)
                {
                    System.err.println("The URL is not correct");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if the option arguments are valid.
     * @param argsArray
     * @return
     */
    private boolean checkArguments(ArrayList<String> argsArray)
    {
        if (argsArray.size() == 0)
            return false;

        boolean check;
        for (String s : argsArray)
        {
            if (s.charAt(0) != '-')
                continue;

            check = false;
            for (String command : COMMANDS)
            {
                if (s.equals(command))
                {
                    check = true;
                    break;
                }
            }
            if (!check)
                return false;
        }
        return true;
    }

    /**
     * Redo an already sent request.
     * @param n number of the request in the directory
     * @param mode Mode of the loading. If its 1, then it also
     *             resends the request
     * @return The loaded request
     */
    public static Request loadRequest(int n, int mode)
    {
        File file = new File("./RequestHistory");
        String[] list = file.list();
        String path = null;

        try
        {
            path = "./RequestHistory/" + list[n];
        }
        catch (IndexOutOfBoundsException e)
        {
            System.err.println("Error: Fire did not work, Number is not valid.");
            System.exit(2);
        }

        try (FileInputStream fi = new FileInputStream(path))
        {
            ObjectInputStream os = new ObjectInputStream(fi);

            Request request = (Request) os.readObject();

            if (mode == 1)
            {
                new Request(request.getUrl(), request.getOptions());
            }

            return request;
        }
        catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Prints a list of saved requests.
     */
    public void listRequestHistory()
    {
        File file = new File("./RequestHistory");
        String[] list = file.list();
        String path;

        for (int i = 0; i < list.length; i++)
        {
            path = "./RequestHistory/" + list[i];
            try (FileInputStream fi = new FileInputStream(path))
            {
                ObjectInputStream os = new ObjectInputStream(fi);

                Request request = (Request) os.readObject();

                System.out.printf("%d url: %s | method: %s | headers: %s | Date: %s%n",
                        i + 1,
                        request.getUrl().toString(),
                        request.getMethod(),
                        request.getRequestHeaders().toString(),
                        request.getDate());

            }
            catch (IOException | ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Prints the help massage.
     */
    public void printHelp()
    {
        System.out.println("Usage: postpost <url> [options...]");
        System.out.println(
                "-M, --method <method>        Set request method\n" +
                "-h, --headers <header/@file> pass custom header(s) to server\n" +
                "-d, --data <name=content>    specify multipart form data\n" +
                "-O, --output <file>          write to file instead of stdout\n" +
                "-S, --save                   save request with its options\n" +
                "-i                           print response headers\n" +
                "-h, --help                   print help\n" +
                "If you want to upload a file use -d \"file=path\"; 'path' is " +
                "your file path.");
    }

    /**
     * Get the sent request.
     * @return The sent request
     */
    public Request getRequest()
    {
        return request;
    }
}
