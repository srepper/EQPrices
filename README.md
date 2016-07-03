# EQPrices
Personal Java project to track of item prices in EverQuest.

This program uses a simple GUI to select available items from a drop-down list and display its average price below in text.

The Item class implements Comparable, overriding the compareTo() method to sort based on the item name (ignoring case).
The ItemCollection class contains a Vector of Item objects, and is sorted using the Collections.sort() method.

Upon running the program, if a CSV is present, the data is parsed into memory.
If any chat logs are present, they are also parsed into memory upon starting the program.

Chat log parsing will continue at set intervals for as long as the program is running.
The initial delay and frequency of chat log parsing are set internally to 10 and 30 minutes respectively.
During each parse, a regular expression is used to identify each line containing an offer.
The item name and price are parsed from each identified line, and the name and price are stored to an Item object.
The Item object has its price averaged while being inserted into the ItemCollection object.
After each chat log has been read and its items parsed into memory, the log file is deleted.
Upon closing the program, all items and their associated average prices are written to the CSV in the same directory as the chat log.
