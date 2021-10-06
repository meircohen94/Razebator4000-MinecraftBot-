package pizdecrp.MCAI;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Proxy;

import com.github.steveice10.mc.protocol.MinecraftProtocol;

import pizdecrp.MCAI.bot.*;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
	    new Thread(() -> {
	        try {
				new Bot(new MinecraftProtocol("bot"), "localhost", 25565, Proxy.NO_PROXY).connect();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }).start();
	}
}
