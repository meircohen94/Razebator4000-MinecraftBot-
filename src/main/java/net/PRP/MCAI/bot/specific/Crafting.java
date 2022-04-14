package net.PRP.MCAI.bot.specific;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.steveice10.mc.protocol.data.game.window.WindowAction;
import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPlaceBlockPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientCloseWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientConfirmTransactionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerCloseWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerConfirmTransactionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowPropertyPacket;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.window.ClickItemParam;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;

import net.PRP.MCAI.Main;
import net.PRP.MCAI.bot.Bot;
import net.PRP.MCAI.data.Vector3D;
import net.PRP.MCAI.utils.BotU;
import net.PRP.MCAI.utils.ThreadU;
import net.PRP.MCAI.utils.VectorUtils;

public class Crafting extends SessionAdapter {
	
	private Bot client;
	public crState state = crState.ENDED;
	public int currentWindowId = 0;
	public WindowType windowType = null;
	public int actionId = 1;
	public int lastactionid = 0;
	public Vector3D craftingBlock = null;
	public craftingRecepie recepie = null;
	public Map<String, craftingRecepie> Recepies = new HashMap<>() {
	private static final long serialVersionUID = -6379467632960849503L;{
		put("planks", new craftingRecepie("inv", "log-.-.-.", new String[] {"log-1"}));
		put("bench",  new craftingRecepie("inv", "planks-planks-planks-planks", new String[] {"planks-4"}));
		put("crafting_table",  new craftingRecepie("inv", "planks-planks-planks-planks", new String[] {"planks-4"}));
		put("sticks",  new craftingRecepie("inv", "planks-.-planks-.", new String[] {"planks-4"}));
		put("stick",  new craftingRecepie("inv", "planks-.-planks-.", new String[] {"planks-4"}));
		put("torch",  new craftingRecepie("inv", "coal-.-stick-.", new String[] {"coal-1","stick-1"}));
		put("wooden_pickaxe",  new craftingRecepie("ct", "planks-planks-planks-.-stick-.-.-stick-.", new String[] {"planks-3","stick-2"}));
		put("stone_pickaxe",  new craftingRecepie("ct", "cobblestone-cobblestone-cobblestone-.-stick-.-.-stick-.", new String[] {"cobblestone-3","stick-2"}));
		put("stone_axe",  new craftingRecepie("ct", ".-cobblestone-cobblestone-.-stick-cobblestone-.-stick-.", new String[] {"cobblestone-3","stick-2"}));
	}};
	public int plitstate = 0;
	public int timeout = 0;
	public int totaltimeouterrors = 0;
	
	
	public class craftingRecepie {
		public String inventoryType;
		public String recepie;
		public String[] needItems;
		public craftingRecepie() {}
		public craftingRecepie(String it, String rec, String[] ni) {this.inventoryType = it; this.recepie = rec; this.needItems = ni;}
		public boolean isInventoried() {return this.inventoryType.equalsIgnoreCase("inv");}
		public boolean isWorkbenched() {return this.inventoryType.equalsIgnoreCase("ct");}
	}
	
	public Crafting(Bot client) {
		this.client = client;
	}
	
	public enum crState {
		START, OPENINGINV, WAITFOROPEN, PLACINGITEMS, GETTINGCRAFTED, ENDED;
	}
	
	@Override
    public void packetReceived(PacketReceivedEvent receiveEvent) {
		if (receiveEvent.getPacket() instanceof ServerOpenWindowPacket) {
			final ServerOpenWindowPacket p = (ServerOpenWindowPacket) receiveEvent.getPacket();
			//System.out.println("sopw "+p.getType());
			currentWindowId = p.getWindowId();
			windowType = p.getType();
			if (state == crState.ENDED) {
				client.getSession().send(new ClientCloseWindowPacket(this.currentWindowId));
			}
			
		} else if (receiveEvent.getPacket() instanceof ServerWindowPropertyPacket) {
			//final ServerWindowPropertyPacket p = (ServerWindowPropertyPacket) receiveEvent.getPacket();
			
			
		} else if (receiveEvent.getPacket() instanceof ServerConfirmTransactionPacket) {
            final ServerConfirmTransactionPacket p = (ServerConfirmTransactionPacket) receiveEvent.getPacket();
            System.out.println("confirmed: "+p.getActionId()+"/"+actionId);
            lastactionid = p.getActionId();
            client.getSession().send(new ClientConfirmTransactionPacket(p.getWindowId(), p.getActionId(), true));
		} else if (receiveEvent.getPacket() instanceof ServerCloseWindowPacket) {
			final ServerCloseWindowPacket p = (ServerCloseWindowPacket) receiveEvent.getPacket();
			//System.out.println("scwp");
			currentWindowId = p.getWindowId();
			windowType = null;
		}
	}
	
	public void setup(String item, Vector3D cb) {
		if (state != crState.ENDED) throw new ConcurrentModificationException("craft already running");
		this.recepie = Recepies.get(item);
		if (!recepie.isInventoried()) {
			this.craftingBlock = cb;
		} else {
			if (Main.debug) System.out.println("d palette:\n"
					+ "#####\n"
					+ "#"+slotstring(1)+"#"+slotstring(2)+"#\n"
					+ "##### ==> "+slotstring(0)+"\n"
					+ "#"+slotstring(3)+"#"+slotstring(4)+"#\n"
					+ "#####"
					);
		}
		this.state = crState.START;
		if (Main.debug) System.out.println("crafitng started: "+item);
	}
	
	public void finish() {
		if (Main.debug) System.out.println("crafitng finished");
		if (windowType != null) client.getSession().send(new ClientCloseWindowPacket(this.currentWindowId));
		recepie = null;
		craftingBlock = null;
		state = crState.ENDED;
		plitstate = 0;
		timeout = 0;
	}
	
	public void finish(String reason) {
		if (Main.debug) System.out.println("crafitng finished: "+reason);
		if (windowType != null) client.getSession().send(new ClientCloseWindowPacket(this.currentWindowId));
		recepie = null;
		craftingBlock = null;
		state = crState.ENDED;
		plitstate = 0;
		timeout = 0;
	}
	
	public int nextActionId() {
		if (actionId >= 100) actionId = 0;
		return actionId++;
	}
	
	public void reset() {
		if (windowType != null) client.getSession().send(new ClientCloseWindowPacket(this.currentWindowId));
		recepie = null;
		craftingBlock = null;
		state = crState.ENDED;
		plitstate = 0;
		totaltimeouterrors = 0;
		timeout = 0;
	}
	
	public void fromSlotToSlotStack(int from, int to) {
		if (client.playerInventory.getSlot(to) != null) {
			client.getSession().send(new ClientWindowActionPacket(client.crafter.currentWindowId,
				nextActionId(),
				from,
				client.playerInventory.getSlot(from), 
				WindowAction.CLICK_ITEM,
				ClickItemParam.LEFT_CLICK
			));
			client.getSession().send(new ClientWindowActionPacket(client.crafter.currentWindowId,
				nextActionId(),
				to,
				client.playerInventory.getSlot(to), 
				WindowAction.CLICK_ITEM,
				ClickItemParam.LEFT_CLICK
			));
			client.getSession().send(new ClientWindowActionPacket(client.crafter.currentWindowId,
				nextActionId(),
				from,
				client.playerInventory.getSlot(from),
				WindowAction.CLICK_ITEM,
				ClickItemParam.LEFT_CLICK
			));
		} else {
			client.getSession().send(new ClientWindowActionPacket(client.crafter.currentWindowId,
				nextActionId(),
				from,
				client.playerInventory.getSlot(from), 
				WindowAction.CLICK_ITEM,
				ClickItemParam.LEFT_CLICK
			));
			client.getSession().send(new ClientWindowActionPacket(client.crafter.currentWindowId,
				nextActionId(),
				to,
				client.playerInventory.getSlot(to), 
				WindowAction.CLICK_ITEM,
				ClickItemParam.LEFT_CLICK
			));
		}
	}
	
	public int i(int slot) {
		if (client.playerInventory.getSlot(slot) == null) {
			return 0;
		} else {
			return client.playerInventory.getSlot(slot).getId();
		}
	}
	
	public void fromSlotToSlot(int from, int to) {
		System.out.println(from+"|"+i(from)+" >>> "+to+"|"+i(to));
		client.getSession().send(new ClientWindowActionPacket(client.crafter.currentWindowId,
			nextActionId(),
			from,
			client.playerInventory.getSlot(from), 
			WindowAction.CLICK_ITEM,
			ClickItemParam.LEFT_CLICK
		));
		client.getSession().send(new ClientWindowActionPacket(client.crafter.currentWindowId,
			nextActionId(),
			to,
			client.playerInventory.getSlot(to), 
			WindowAction.CLICK_ITEM,
			ClickItemParam.RIGHT_CLICK
		));
		client.getSession().send(new ClientWindowActionPacket(client.crafter.currentWindowId,
			nextActionId(),
			from,
			client.playerInventory.getSlot(from),
			WindowAction.CLICK_ITEM,
			ClickItemParam.LEFT_CLICK
		));
	}
	
	public void ShiftClick(int slot) {
		client.getSession().send(new ClientWindowActionPacket(client.crafter.currentWindowId,
			client.crafter.nextActionId(),
			slot,
			client.playerInventory.getSlot(slot),
			WindowAction.SHIFT_CLICK_ITEM,
			ClickItemParam.LEFT_CLICK
		));
	}
	
	@SuppressWarnings("deprecation")
	public void tick() {
		if (totaltimeouterrors >= 2) {
			client.reconnect();
		}
		try {
			if (state == crState.START) {
				if (recepie == null) {
					finish("nullrecepie");
					return;
				} else if (recepie.isInventoried()) {
					plitstate = 8;
					for (String item : recepie.needItems) {
						if (!client.playerInventory.contain(item.split("-")[0], Integer.parseInt(item.split("-")[1]))) {
							finish("noitems");
							return;
						}
					}
					//all OK
					//ThreadU.sleep(20);
					state = crState.PLACINGITEMS;
				} else if (recepie.isWorkbenched()) {
					plitstate = 18;
					for (String item : recepie.needItems) {
						if (!client.playerInventory.contain(item.split("-")[0], Integer.parseInt(item.split("-")[1]))) {
							finish("noitems");
							return;
						}
					}
					state = crState.OPENINGINV;
				}
			} else if (state == crState.OPENINGINV) {
				if (recepie.isWorkbenched()) {
					if (craftingBlock == null) {
						craftingBlock = findblockByName(client,151);
						if (craftingBlock == null) {
							finish();
							return;
						} else {
							return;
						}
					}
					if (VectorUtils.sqrt(client.getEyeLocation(), craftingBlock) <= (int)Main.getsett("maxpostoblock")) {
						BotU.LookHead(client, craftingBlock);
						client.getSession().send(new ClientPlayerPlaceBlockPacket(craftingBlock.translate(), BlockFace.UP, Hand.MAIN_HAND, 0.5F, 1F, 0.5F, false));
						client.getSession().send(new ClientPlayerSwingArmPacket(Hand.MAIN_HAND));
						state = crState.WAITFOROPEN;
					} else {
						//System.out.println(craftingBlock+" -5 sqrt:"+VectorUtils.sqrt(client.getPosition(), craftingBlock));
						finish("craftblock_too_far");
						return;
					}
				}
				
			} else if (state == crState.WAITFOROPEN) {
				if (recepie.isInventoried()) {
					//dammnnn wtf
				} if (recepie.isWorkbenched()) {
					if (windowType == WindowType.CRAFTING) {
						state = crState.PLACINGITEMS;
					} else {
						timeout++;
						if (timeout > 200) {
							finish("waitforinventory_timeout");
						}
					}
				}
				
			} else if (state == crState.PLACINGITEMS) {
				
				if (recepie.isInventoried()) {
					if (plitstate <= 0) {
						state = crState.GETTINGCRAFTED;
						return;
					}
					if (plitstate%2 == 1) {
						plitstate--;
					} else {
						int i = plitstate / 2;
						if (!recepie.recepie.split("-")[i-1].equalsIgnoreCase(".")) fromSlotToSlot(client.playerInventory.getSlotWithItem(recepie.recepie.split("-")[i-1]), i);
						plitstate--;
					}
				} else if (recepie.isWorkbenched()) {
					if (plitstate <= 0) {
						state = crState.GETTINGCRAFTED;
						return;
					}
					if (plitstate%2 == 1) {
						plitstate--;
					} else {
						int i = plitstate / 2;
						System.out.println(client.playerInventory.getSlotWithItem(recepie.recepie.split("-")[i-1]));
						if (!recepie.recepie.split("-")[i-1].equalsIgnoreCase(".")) fromSlotToSlot(client.playerInventory.getSlotWithItem(recepie.recepie.split("-")[i-1]), i);
						plitstate--;
					}
				}
				
			} else if (state == crState.GETTINGCRAFTED) {
				if (recepie.isInventoried()) {
					if (client.playerInventory.getSlot(0) != null) {
						ShiftClick(0);
						finish("normal");
						return;
					} else {
						timeout++;
						if (timeout > 200) {
							totaltimeouterrors++;
							finish("getcrafted_timeout, palette:\n"
									+ "#####\n"
									+ "#"+slotstring(1)+"#"+slotstring(2)+"#\n"
									+ "##### ==> "+slotstring(0)+"\n"
									+ "#"+slotstring(3)+"#"+slotstring(4)+"#\n"
									+ "#####"
									);
							ShiftClick(1);
							ThreadU.sleep(50);
							ShiftClick(2);
							ThreadU.sleep(50);
							ShiftClick(3);
							ThreadU.sleep(50);
							ShiftClick(4);
						}
					}
				} else if (recepie.isWorkbenched()) {
					if (client.playerInventory.getSlot(0) != null) {
						ShiftClick(0);
						finish();
						return;
					} else {
						timeout++;
						if (timeout > 200) {
							totaltimeouterrors++;
							ShiftClick(1);
							ThreadU.sleep(20);
							ShiftClick(2);
							ThreadU.sleep(20);
							ShiftClick(3);
							ThreadU.sleep(20);
							ShiftClick(4);
							ThreadU.sleep(20);
							ShiftClick(5);
							ThreadU.sleep(20);
							ShiftClick(6);
							ThreadU.sleep(20);
							ShiftClick(7);
							ThreadU.sleep(20);
							ShiftClick(8);
							ThreadU.sleep(20);
							ShiftClick(9);
							//if (windowType != null) client.getSession().send(new ClientCloseWindowPacket(this.currentWindowId));
							finish("getcrafted_timeout");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			finish("unknownerror");
		}
	}
	
	public String slotstring(int slot) {
		ItemStack s = client.playerInventory.getSlot(slot);
		if (s == null) return ".";
		return s.getId()+"";
	}
	
	public Vector3D findblockByName(Bot client, int id) {
    	List<Vector3D> positions = new CopyOnWriteArrayList<>();
    	Vector3D ps = client.getPositionInt();
    	int x = (int)ps.getPosX();
    	int y = (int)ps.getPosY();
    	int z = (int)ps.getPosZ();
    	int radius = 5;
    	Vector3D pos = null;
    	for (int i = 1; i <= radius; i++) {
    		int xs = x-i;
    		int ys = y-i;
    		if (ys < 0) ys = 0;
    		int yi = y+i;
    		if (yi > 256) yi = 256;
    		//System.out.println((int)client.posY+"  min y:"+ys+" max y:"+yi);
    		int zs = z-i;
    		for (int y1 = ys; y1 < yi; y1++) {
    			for (int x1 = xs; x1 < x+i; x1++) {
                    for (int z1 = zs; z1 < z+i; z1++) {
                    	pos = new Vector3D(x1,y1,z1);
                    	//System.out.println(a.getBlock(client).id+" != "+id+" pos:"+a.toStringInt());
                		if (pos.getBlock(client) != null && pos.getBlock(client).id == id) {
                			positions.add(pos);
                		}
                    }
                }
            }
    	}
    	pos = VectorUtils.getNear(client.getPositionInt(),positions);
    	return pos;
    }
}