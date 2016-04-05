package nl.function1.polepass;

/*
 * PoleCompass 2.0.2 - Minecraft Bukkit/Spigot plugin by joppiesaus <job@function1.nl>
 * License: Public Domain. Do whatever you want. I'd love to hear what you're making with my code, though!
 * 
 * TODO:
 * GUI
 * CLICK DIRECTIONS
 * CLICK ENTITY TO FOLLOW?
 * 
 * CHANGELOG:
 * Learned that I should, I definitely should, test more. 
 * Fixed not being able to reset bug, the other variant
 * Fixed "<player> is not a valid command or direction" on /setplayercompass
 */

import java.util.List;

import org.bukkit.metadata.Metadatable;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
//import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.event.Listener;
//import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;


public final class Compass extends JavaPlugin implements Listener {
	
	public void onEnable() {
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
	}
	
	public void setMetadata(Metadatable object, String key, Object value, Plugin plugin) {
		object.setMetadata(key, new FixedMetadataValue(plugin, value));
	}
	
	public void removeMetadata(Metadatable object, String key, Plugin plugin) {
		object.removeMetadata(key, plugin);
	}

	public Object getMetadata(Metadatable object, String key, Plugin plugin) {
		List<MetadataValue> values = object.getMetadata(key);  
		for (MetadataValue value : values) {
			if (value.getOwningPlugin() == plugin) {
				return value.value();
			}
		}
		return null;
	}
	
	private void addTracker(Player requester, Player toTrack) {
		String data = (String)getMetadata(toTrack, "compasstarget", this);
		if (data == null) {
			data = requester.getName();
		} else {
			data += "," + requester.getName();
		}
		setMetadata(toTrack, "compasstrackers", data, this);
		
		setMetadata(requester, "compasstracking", toTrack.getName(), this);
	}
	
	private void removeRequesterFromTrackingOnly(Player p, String requester) {
		String data = (String)getMetadata(p, "compasstrackers", this);
		
		if (data == null) {
			return;
		}
		
		String[] players = data.split(",");
		
		String newData = "";
		for (String player : players) {
			if (player != requester) {
				newData += player + ",";
			}
		}
		
		if (newData == "") {
			removeMetadata(p, "compasstrackers", this);
		} else {
			newData = newData.substring(newData.length() - 2, newData.length() - 1);
			setMetadata(p, "compasstrackers", newData, this);
		}
	}
	
	private void removeTrackerFromRequester(Player requester) {
		String toTrack = (String)getMetadata(requester, "compasstracking", this);
		removeMetadata(requester, "compasstracking", this);
		
		if (toTrack == null) {
			return;
		}
		Player p = Bukkit.getServer().getPlayer(toTrack);
		
		if (p != null)
		{
			removeRequesterFromTrackingOnly(p, requester.getName());
 		}
	}
		
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if (cmd.getName().equalsIgnoreCase("compass")) {
			
			if (!(sender instanceof Player)) {
				sender.sendMessage("You must be a player to set your own compass!");
				return false;
			}
			
			if (args.length < 1) {
				sender.sendMessage("No arguments specified! View \"compass help\" for help.");
				return false;
			}
			
			Player p = (Player) sender;
			Direction toSet = null;
			
			/*// a meta data test
			Metadatable dataff = (Metadatable) p;
			setMetadata(dataff, "JAMES", "BOND", this);
			p.sendMessage((String) getMetadata(dataff, "JAMES", this));
			*/
			
			switch (args[0].toLowerCase()) {
					
				case "help":
					p.sendMessage(ChatColor.GOLD + "/compass [direction]" + ChatColor.GRAY + 
							" - sets your compass direction to North, West, East or South.");
					p.sendMessage(ChatColor.GOLD + "/compass pos <x> <y> <z>" + ChatColor.GRAY + 
							" - sets your compass direction to the specific location.");
					p.sendMessage(ChatColor.GOLD + "/compass pos current" + ChatColor.GRAY + " - sets your compass direction to your current position.");
					p.sendMessage(ChatColor.GOLD + "/compass follow <player>" + ChatColor.GRAY + " - sets your compass direction to someone else's" +
							" position. Keeps updating.");
					//p.sendMessage(ChatColor.GOLD + "/compass about" + ChatColor.GRAY + " - views the info of the plugin.");
					p.sendMessage(ChatColor.GOLD + "To make your compass normal again, use /compass reset.");
					p.sendMessage(ChatColor.GOLD + "To modify someone else's compass, use /setplayercompass and then anything listed here.");
					return true;
					
				case "about":
					PluginDescriptionFile pdf = this.getDescription();
					String name = "PoleCompass";
					String des = pdf.getDescription();
					String ver = pdf.getVersion();
					String url = pdf.getWebsite();
					String aut = pdf.getAuthors().get(0);
					
					if (sender instanceof Player) {	
						p.sendMessage(ChatColor.AQUA + name + " " + ver);
						p.sendMessage(ChatColor.GOLD + des);
						p.sendMessage(ChatColor.GOLD + "Author: " + ChatColor.RESET + aut);
						p.sendMessage(ChatColor.GOLD + "URL: " + ChatColor.RESET + url);
					} else {
						// TODO: Will never be true. Fix this. I'm lazy.
						sender.sendMessage(name + " " + ver);
						sender.sendMessage(des);
						sender.sendMessage("Author: " + aut);
						sender.sendMessage("URL: " + url);
					}
					return true;
					
				case "follow":
				case "f":
					if (args.length < 2) {
						p.sendMessage(ChatColor.RED + "Syntax error: Please enter a player to point to with your compass");
						return false;
					}
					
					Player toFollow = Bukkit.getServer().getPlayer(args[1]);
					
					if (toFollow != null) {
						
						p.setCompassTarget(toFollow.getLocation());
						addTracker(p, toFollow);
						p.sendMessage("You now follow " + toFollow.getName() + " with your compass!");
						return true;						
						
					} else {
						p.sendMessage(ChatColor.RED + "\"" + args[1] + "\" is not found on this server");
						return false;
					}
					
				case "pos":
					if (args.length < 2) {
						p.sendMessage(ChatColor.RED + "Usage: /compass pos current or /compass pos <x> <y> <z>");
						return false;
					}
					if (args[1].toLowerCase().startsWith("c")) {
						p.setCompassTarget(p.getLocation());						
						p.sendMessage("Your compass is now pointing to your current location.");
						return true;
					}
					if (args.length < 4) {
						p.sendMessage(ChatColor.RED + "Usage: /compass pos current or /compass pos <x> <y> <z>");
						return false;
					}
					
					try {
						int x = Integer.parseInt(args[1]);
						int y = Integer.parseInt(args[2]);
						int z = Integer.parseInt(args[3]);
						p.setCompassTarget(p.getWorld().getBlockAt(x,y,z).getLocation());
						p.sendMessage("Your compass is now pointing to " + x + ", " + y + ", " + z);
						return true;
					} catch (Exception ex) {
						p.sendMessage("Failed to convert positions");
						return false;
					}	
					
				case "n":
				case "north":
					toSet = Direction.North;
					break;
				
				case "e":
				case "east":
					toSet = Direction.East;
					break;
					
				case "s":
				case "south":
					toSet = Direction.South;
					break;
				
				case "w":
				case "west":
					toSet = Direction.West;
					break;
				
				case "r":
				case "reset":
					toSet = Direction.None;
					removeTrackerFromRequester(p);
					break;
					
				default:
					
					break;
			}
			
			if (toSet != null) {
				setCompassToDirection(p, toSet);
				return true;
			} else {
				p.sendMessage(ChatColor.RED + "\"" + args[0] + "\" is not a valid direction.");
				return false;
			}
		} else if (cmd.getName().equalsIgnoreCase("setplayercompass")) {			
						
			if (args.length < 2) {
				sender.sendMessage("Syntax error: Too few arguments!");
				return false;
			}
			
			Player target = Bukkit.getServer().getPlayer(args[0]); 
			
			if (target == null) {
				sender.sendMessage("Error: Player \"" + args[0] + "\" not found");
				return false;
			}
			
			Direction toSet = null;
			
			switch (args[1].toLowerCase()) {
			
				case "help":
					sender.sendMessage("no");
					return true;
				
				case "f":
				case "follow":
					if (args.length < 3) {
						sender.sendMessage("Syntax error: missing player to follow");
						return false;
					}
					
					Player toFollow = Bukkit.getServer().getPlayer(args[2]);
					
					if (toFollow == null) {
						sender.sendMessage("Player \"" + args[2] + " not found");
						return false;
					}
					
					target.setCompassTarget(toFollow.getLocation());
					addTracker(target, toFollow);
					sender.sendMessage(target.getName() + "'s compass is now pointing to " + toFollow.getName());
					return true;
					
				case "pos":
					if (args.length < 2) {
						sender.sendMessage("Syntax error: no location specified");
						return false;
					}
					
					if (args[2].toLowerCase().startsWith("c")) {
						target.setCompassTarget(target.getLocation());
						sender.sendMessage(target.getName() + "'s compass is now pointing to " + target.getName() + " current location");
						return true;
					}
					if (args.length < 5) {
						sender.sendMessage(ChatColor.RED + "Usage: /compass pos current or /compass pos <x> <y> <z>");
						return false;
					}
					
					try {
						int x = Integer.parseInt(args[2]);
						int y = Integer.parseInt(args[3]);
						int z = Integer.parseInt(args[4]);
						target.setCompassTarget(target.getWorld().getBlockAt(x,y,z).getLocation());
						sender.sendMessage(target.getName() + "'s compass is now pointing to " + x + ", " + y + ", " + z);
						return true;
					} catch (Exception ex) {
						sender.sendMessage("Failed to convert positions");
						return false;
					}
					
				case "n":
				case "north":
					toSet = Direction.North;
					break;
				
				case "e":
				case "east":
					toSet = Direction.East;
					break;
					
				case "s":
				case "south":
					toSet = Direction.South;
					break;
				
				case "w":
				case "west":
					toSet = Direction.West;
					break;
				
				case "r":
				case "reset":
					toSet = Direction.None;
					removeTrackerFromRequester(target);
					break;
					
				default:
					
					break;
			}
			
			if (toSet != null) {
				setCompassToDirection(target, toSet);
				return true;
			} else {
				sender.sendMessage("Syntax error: \"" + args[1] + "\" is not a valid direction or command");
				return false;
			}		
		}
		return false;
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		
		Object data = getMetadata(p, "compasstrackers", this);
		if (data == null) {
			return;
		}
		
		String[] players = ((String)data).split(",");	
				
		for (String pr : players) {
			Player toUpdate = Bukkit.getServer().getPlayer(pr);
			if (toUpdate != null) {
				toUpdate.setCompassTarget(p.getLocation());
			} else {
				removeRequesterFromTrackingOnly(p, pr);
			}
		}
	}
	
	/*// for a future release
	@EventHandler
	public void onPlayerUse(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		
		// TODO: compatibility with 1.8
		if (!(p.getInventory().getItemInMainHand().getType() == Material.COMPASS || p.getInventory().getItemInOffHand().getType() == Material.COMPASS)) {
			return;
		}
		
		if (getMetadata(p, "poledirection", this) != null) {
			// Tell what direction the player is facing
			float yaw = p.getLocation().getYaw();
			
			p.sendMessage("You're currently facing " + whatDirection(yaw) + "(" + yaw + ")");
		} else {
			// Tell how far away in a certain direction the player from it's target is
			
			Location pp = p.getLocation();
			Location cp = p.getCompassTarget();
			
			double px = pp.getX();
			double pz = pp.getZ();
			
			double cx = cp.getX();
			double cz = cp.getZ();
			
			double xd = Math.abs(px - cx);
			double zd = Math.abs(pz - cz);
			
			double d = Math.sqrt(Math.pow(xd, 2) + Math.pow(zd, 2));
			long distance = Math.round(d);
			
			if (px > cx) {
				// target is relatively west
			} else {
				// target is relatively east
			}
			
			if (pz > cz) {
				// target is relatively north
			} else {
				// target is relatively south
			}
			
			
			// directions
			p.sendMessage(ChatColor.GOLD + "UR " + distance + " BLACKS AWEAY FROM TARGET");
			p.sendMessage(ChatColor.GOLD + "TARGET IS " + Math.round(xd) + " " + (px > cx ? "west" : "east") + " of you");
			p.sendMessage(ChatColor.GOLD + "TARGET IS " + Math.round(zd) + " " + (pz > cz ? "north" : "south") + " of you");
		}		
	}
	
	private String whatDirection(float angle) {
		String result = "";
		
		// TODO: Fix neg values
		angle = Math.abs(angle);
		
		if (angle < 45.0f || angle > 315.0f) {
			return "south";
		} else if (angle < 135.0f) {
			// west
			return "west";
		} else if (angle < 225.0f){
			//north
			return "north";
		} else if (angle < 315.0f) {
			//east
			return "east";
		}

		return result;
	}*/
	
	private void setCompassToDirection(Player p, Direction direction) {
		Location loc = p.getLocation();
		
		switch (direction) {
			case North:
				p.setCompassTarget(p.getWorld().getBlockAt((int)loc.getX(), 0, -12550820).getLocation());
				break;
			case East:
				p.setCompassTarget(p.getWorld().getBlockAt(12550820, 0, (int)loc.getZ()).getLocation());
				break;
			case South:
				p.setCompassTarget(p.getWorld().getBlockAt((int)loc.getX(), 0, 12550820).getLocation());
				break;
			case West:
				p.setCompassTarget(p.getWorld().getBlockAt(-12550820, 0, (int)loc.getZ()).getLocation());
				break;
			default:
				if (p.getBedSpawnLocation() != null) {
					p.setCompassTarget(p.getBedSpawnLocation());
				} else {
					p.setCompassTarget(p.getWorld().getSpawnLocation());
				}
				//removeMetadata(p, "poledirection", this);
				return;
		}
		
		//setMetadata(p, "poledirection", true, this);
	}
	
	private enum Direction {
		None, North, East, South, West;
	}
}
