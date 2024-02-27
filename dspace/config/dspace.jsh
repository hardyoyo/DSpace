// JShell startup script to pre-load DSpace-related classes into the JShell REPL for DSpace

// Import commonly used DSpace classes
import org.dspace.core.Context;
import org.dspace.content.Item;
import org.dspace.content.Collection;
import org.dspace.content.Community;

// Create a new DSpace context
Context context = new Context();

// Example function to create a new item
public Item createNewItem() {
    Item newItem = Item.create(context);
    // Additional setup for the new item can go here
    return newItem;
}

System.out.println("DSpace JShell startup script loaded successfully.");
