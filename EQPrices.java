import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Collections;
import java.util.Vector;
import javax.swing.*;

public class EQPrices extends JFrame
{
	private static final long serialVersionUID = 1L;
	private final File PATH = new File("C:\\Users\\Stephen\\AppData\\Local\\VirtualStore\\Program Files (x86)\\Sony\\EverQuest\\Logs");
	private final String LOG = "eqlog";
	private final File DATA = new File(PATH + "\\pricelist.csv");
	private static final String VendorText = ".*tells you, 'I'll give you ((\\d* (copper|silver|gold|platinum) ){1,4}|absolutely nothing )(for the|per).*";
	private static final String AuctionText = ".*auctions, '(?i:WTS) (.*\\d+?(\\.d*)??[(?i:k)]?(?i:p){0,2})+?.*";
	private final int SECONDSPERMINUTE = 60;
	private final int MILLISECONDS = 1000;
	private final int delay = 10 * SECONDSPERMINUTE * MILLISECONDS;
	private final int frequency = 30 * SECONDSPERMINUTE * MILLISECONDS;
	private ItemCollection priceData = new ItemCollection();
	private JPanel mainPanel = new JPanel();
	private SelectionPanel itemList;
	private PricePanel pricePanel = new PricePanel();
	
	public static void main(String args[])
	{
		EQPrices app = new EQPrices();
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		app.setTitle("EQ Prices");
		app.setResizable(false);
		app.pack();
		app.setLocationRelativeTo(null);
		app.setVisible(true);
	}
	
	public EQPrices()
	{	
		parseData();
		parseLogs();
		addItemListListener();
		
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				writeData();
			}
		});		
		
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		mainPanel.add(itemList);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
		mainPanel.add(pricePanel);
		
		add(mainPanel);
		
		ActionListener timedTask = new ActionListener(){
			public void actionPerformed(ActionEvent e){
				parseLogs();
				addItemListListener();
				writeData();
			}
		};
		
		Timer timer = new Timer(frequency, timedTask);
		timer.setInitialDelay(delay);
		timer.start();
	}
	
	public void addItemListListener()
	{
		itemList.getList().addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				int price = (int)priceData.getPrice(itemList.getItemIndex());
				pricePanel.setPrice(price);
				pack();
			}
		});
	}
	
	public void parseData()
	{
		BufferedReader input;
		String line = "";
		
		try{
			input = new BufferedReader(new FileReader(DATA));
			
			while((line = input.readLine()) != null)
			{
				String[] values = line.split(",");
				double p = Double.parseDouble(values[1]);
				
				Item i = new Item(values[0], p);
				priceData.addItem(i);
			}			
			priceData.sort();
			
			input.close();
		}
		catch(Exception e){
			System.out.println(e.toString());
		}
	}
	
	public void parseLogs()
	{
		BufferedReader input;
		String line = "";
		String itemName = "";
		int price = 0;
		
		File[] logs = PATH.listFiles(new FileFilter(){
			
			@Override
			public boolean accept(File file)
			{
				return file.getName().startsWith(LOG);
			}
		});
		
		for(int i = 0; i < logs.length; ++i)
		{
			try{
				input = new BufferedReader(new FileReader(logs[i]));
				
				while((line = input.readLine()) != null)
				{
					if(line.matches(VendorText))
					{
						line = line.substring(line.lastIndexOf("you") + 4);
						itemName = getItemName(line);
						
						if(line.contains("absolutely nothing"))
							price = 0;
						else
							price = getItemPrice(line);
						
						priceData.addSortAndAverage(new Item(itemName, price));
					}
				}
				
				line = "";
				input.close();
				logs[i].delete();				
			} catch(Exception e){
				System.out.println(e.toString());
			}
		}
		
		if(itemList != null)
		{
			int index = itemList.getItemIndex();
			itemList.rebuildList(priceData.getStringList(), index);
		}
		else
		{
			itemList = new SelectionPanel(priceData.getStringList());
			itemList.getList().actionPerformed(
					new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
		}
		
		pack();
	}
	
	public String getItemName(String str)
	{
		if(str.contains(" per "))
		{
			str = str.substring(str.indexOf(" per ") + 5, str.length() - 1);
		}
		else
			str = str.substring(str.indexOf("for the") + 8, str.length() - 1);
		
		if(str.endsWith("."))
		{
			str = str.substring(0, str.length() - 1);
		}
		return str;
	}
	
	public int getItemPrice(String str)
	{
		int price = 0;
		int digit = 0;
		String[] values;
		
		if(str.contains(" per "))
		{
			str = str.substring(0, str.indexOf(" per "));
		}
		else
			str = str.substring(0, str.indexOf(" for the "));
		
		values = str.split(" ");
		for(int i = 0; i < values.length; i += 2)
		{
			digit = 0;
			if(values[i + 1].contentEquals("copper"))
				digit = Integer.parseInt(values[i]);
			else if(values[i + 1].contentEquals("silver"))
				digit = Integer.parseInt(values[i]) * 10;
			else if(values[i + 1].contentEquals("gold"))
				digit = Integer.parseInt(values[i]) * 100;
			else if(values[i + 1].contentEquals("platinum"))
				digit = Integer.parseInt(values[i]) * 1000;
			
			price += digit;
		}
		
		return price;
	}
	
	public void writeData()
	{
		File backup = new File(DATA + ".bk");
		if(backup.exists())
			backup.delete();
		
		File data = new File(DATA + "");
		if(data.exists())
		{
			if(!data.renameTo(backup))
				JOptionPane.showMessageDialog(null, "Failed to backup old data.", 
					"Error", JOptionPane.ERROR_MESSAGE);
		
			data.delete();
		}
		
		data = new File(DATA + "");
		try{
			BufferedWriter output = new BufferedWriter(new FileWriter(data.getAbsoluteFile()));
			String line = "";
			
			for(int i = 0; i < priceData.size(); ++i)
			{
				line += priceData.get(i).getName();
				line += ",";
				line += priceData.get(i).getPrice();
				
				if(i != priceData.size() - 1)
					line += "\n";
				
				output.write(line);
				line = "";
			}
			
			output.close();
		} catch(Exception e){
			System.out.println(e.toString());
		}
	}
}

class SelectionPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private JComboBox<String> itemList = new JComboBox<String>();
	
	public SelectionPanel(Vector<String> items)
	{
		itemList = new JComboBox<String>(items);
		itemList.setEditable(true);
		itemList.setSelectedItem("Select item");
		itemList.setEditable(false);
		add(itemList);
	}
	
	public int getItemIndex()
	{
		return itemList.getSelectedIndex();
	}
	
	public JComboBox<String> getList()
	{
		return itemList;
	}
	
	public void rebuildList(Vector<String> items, int index)
	{
		remove(itemList);
		itemList = new JComboBox<String>(items);
		itemList.setSelectedIndex(index);
		if(index == -1)
		{
			itemList.setEditable(true);
			itemList.setSelectedItem("Select item");
			itemList.setEditable(false);
		}
		add(itemList);
	}
}

class PricePanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	JLabel name = new JLabel();
	JLabel price = new JLabel();
	
	public PricePanel()
	{
		setPrice(0);
		setLayout(new BorderLayout(10,10));
		add(new JPanel().add(name), BorderLayout.NORTH);
		add(new JLabel("Average item price:"), BorderLayout.CENTER);
		add(price, BorderLayout.EAST);
	}
	
	public void setPrice(int newPrice)
	{
		price.setText(priceToString(newPrice));
	}
	
	public String priceToString(int newPrice)
	{
		String result = "";
		int nextValue = 0;
		
		for(int i = 0; i < 3; ++i)
		{
			nextValue = (int)(newPrice % 10);
			newPrice /= 10;
			result = Integer.toString(nextValue) + getCurrency(i) + result; 
		}
		result = Integer.toString(newPrice) + getCurrency(3) + result;
		return result;
	}
	
	public String getCurrency(int digit)
	{
		switch(digit)
		{
			case 0 :
			{
				return "cp ";
			}
			case 1 :
			{
				return "sp ";
			}
			case 2 :
			{
				return "gp ";
			}
			case 3 :
			{
				return "pp ";
			}
			default :
			{
				return "-";
			}
		}
	}
}

class Item implements Comparable<Item>
{
	String name;
	double price;
	
	public Item(String n, double p)
	{
		name = n;
		price = p;
	}
	
	public Item(String n, double p, int s)
	{
		name = n;
		price = p;
	}
	
	public String getName()
	{
		return name;
	}
	
	public double getPrice()
	{
		return price;
	}
	
	public void setPrice(double newPrice)
	{
		price = newPrice;
	}
	
	@Override
	public int compareTo(Item i)
	{
		int x = name.compareToIgnoreCase(i.getName());
		return x;
	}
}

class ItemCollection
{
	Vector<Item> list;
	
	public ItemCollection()
	{
		list = new Vector<Item>();
	}
	
	public void addItem(Item i)
	{
		int index = 0;
		while(index < list.size() && i.compareTo(list.get(index)) > 0)
			index++;
		
		if(index == list.size() || i.compareTo(list.get(index)) != 0)
			list.add(i);
		else
		{			
			JOptionPane.showMessageDialog(null, "Duplicate data found in parseData().", 
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void addItemWithAverage(Item i)
	{
		int index = 0;
		while(index < list.size() && i.compareTo(list.get(index)) > 0)
			index++;
		
		if(index == list.size() || i.compareTo(list.get(index)) != 0)
			list.add(i);
		else
		{			
			if(i.getPrice() == list.get(index).getPrice())
				return;
			
			double newPrice = rollingAverage(list.get(index).getPrice(), i.getPrice());
			list.get(index).setPrice(newPrice);
		}
	}
	
	public void addSortAndAverage(Item i)
	{
		addItemWithAverage(i);
		sort();
	}
	
	public Item get(int index)
	{
		return list.get(index);
	}
	
	public double getPrice(int itemIndex)
	{
		return list.get(itemIndex).getPrice();
	}
	
	public Vector<String> getStringList()
	{
		Vector<String> v = new Vector<String>();
		
		for(int i = 0; i < list.size(); ++i)
		{
			v.add(list.get(i).getName());
		}
		
		return v;
	}
	
	public double rollingAverage(double avg, double newPrice)
	{
		double newAvg = avg - avg / 100;
		newAvg += newPrice / 100;
		
		return newAvg;
	}
	
	public void sort()
	{
		Collections.sort(list);
	}
	
	public int size()
	{
		return list.size();
	}
}