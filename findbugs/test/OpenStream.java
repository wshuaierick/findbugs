import java.io.*;

public class OpenStream {
	public static void main(String[] argv) throws Exception {
		FileInputStream in = null;

		try {
			in = new FileInputStream(argv[0]);
		} finally {
			// Not guaranteed to be closed here!
			if (Boolean.getBoolean("inscrutable"))
				in.close();
		}

		FileInputStream in2 = null;
		try {
			in2 = new FileInputStream(argv[1]);
		} finally {
			// This one will be closed
			if (in2 != null)
				in2.close();
		}

		// oops!  exiting the method without closing the stream
	}

	public void doNotReport() {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(b);

		out.println("Hello, world!");
	}

	public void systemIn() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println(reader.readLine());
	}

	public void socket(java.net.Socket socket) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		System.out.println(reader.readLine());
	}
}
