package net.PRP.MCAI.ListenersForServers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerUseItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMapDataPacket;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;

import net.PRP.MCAI.Main;
import net.PRP.MCAI.ListenersForServers.holyworld.mode;
import net.PRP.MCAI.bot.Bot;
import net.PRP.MCAI.data.MinecraftData;
import net.PRP.MCAI.utils.BotU;
import net.PRP.MCAI.utils.MapUtils;
import net.PRP.MCAI.utils.StringU;
import net.PRP.MCAI.utils.ThreadU;

public class dexland extends SessionAdapter implements ServerListener {
	
	Bot client;//mc.dexland.su:25565

	boolean allGameCapt = false;
	private mde mod = mde.NON;
	
	public enum mde {
		NON,PICKSERVER, PICKANARCHY
	}
	
	public dexland(Bot client) {
		this.client = client;
		client.getSession().addListener(this);
		client.catchedRegister = true;
	}
	
	@Override
	public void packetReceived(PacketReceivedEvent receiveEvent) {
		//BotU.log(receiveEvent.getPacket().getClass().getName());
		if (receiveEvent.getPacket() instanceof ServerChatPacket) {
			String message = StringU.componentToString(((ServerChatPacket)receiveEvent.getPacket()).getMessage());
			//Main.write("[undecoded msg]", ((ServerChatPacket)receiveEvent.getPacket()).getMessage().toString());
			BotU.log("message received: "+message);
			Main.write("[msg] ", message);
			if (message.contains("/reg")) {
				BotU.chat(client,"/reg 112233asdasd 112233asdasd");
			} else if (message.contains("/l")) {
				BotU.chat(client,"/login 112233asdasd");
			}
		} else if (receiveEvent.getPacket() instanceof ServerMapDataPacket) {
			ServerMapDataPacket p = (ServerMapDataPacket)receiveEvent.getPacket();
			JFrame frame = new JFrame("captcha");
			frame.setSize(300, 380);
			BufferedImage image = MapUtils.mapToPng(p);
	        JLabel l = new JLabel(new ImageIcon(image));
	        l.setBounds(0, 0, 256, 256);
	        
	        JTextField b = new JTextField();
	        b.setBounds(0,310,60,20);
		    frame.add(b);
	        
	        
	        JButton enter = new JButton("отправить");
	        enter.setBounds(65, 310, 120, 20);
	        enter.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					BotU.chat(client, b.getText());
				}
		    });
		    frame.add(enter);
		    frame.add(l);
	        
	        frame.setVisible(true);
		} else if (receiveEvent.getPacket() instanceof ServerSetSlotPacket) {
			ServerSetSlotPacket p = (ServerSetSlotPacket) receiveEvent.getPacket();
			if (p.getSlot() == 36) {
				if (p.getItem() != null && p.getItem().getNbt() != null && p.getItem().getNbt().get("display").toString().contains("Выбор серверов")) {
					BotU.SetSlot(client, 0);
					BotU.log("cs");
					client.getSession().send(new ClientPlayerUseItemPacket(Hand.MAIN_HAND));
				}
				/*for (Tag nbt : p.getItem().getNbt()) {
					BotU.log("name:"+nbt.getName()+", value:"+nbt.getValue());
				}*/
			} else if (p.getSlot() == 29 && p.getItem() != null && Main.getMCData().items.get(p.getItem().getId()).name.contains("tnt")) {
				new Thread(()->{
					ThreadU.sleep(1500);
					client.crafter.click(29);
					BotU.log("ps");
				}).start();
			} else if (p.getSlot() == 16 && p.getItem() != null && Main.getMCData().items.get(p.getItem().getId()).name.contains("tnt")) {
				new Thread(()->{
					ThreadU.sleep(1500);
					client.crafter.click(16);
					mod = mde.NON;
					BotU.log("pa");
					ThreadU.sleep(3000);
					BotU.chat(client, "/call __Flashback__");
				}).start();
			}
		} else if (receiveEvent.getPacket() instanceof ServerOpenWindowPacket) {
			if (mod == mde.NON) return;
			final ServerOpenWindowPacket p = (ServerOpenWindowPacket) receiveEvent.getPacket();
			BotU.log("inv name: "+p.getName());
			Main.write("[inv]", "inv name: "+p.getName());
			if (p.getName().contains("Выбор режима")) {
				mod = mde.PICKSERVER;
				BotU.log("sps");
			} else if (p.getName().contains("Выбор АНАРХИИ")) {
				mod = mde.PICKANARCHY;
				BotU.log("spa");
			}
		}
	}

	@Override
	public void tick() {
		// TODO Auto-generated method stub
		
	}

}