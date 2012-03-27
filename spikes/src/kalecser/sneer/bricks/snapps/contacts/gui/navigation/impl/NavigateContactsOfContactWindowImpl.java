package kalecser.sneer.bricks.snapps.contacts.gui.navigation.impl;

import static basis.environments.Environments.my;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;

import kalecser.sneer.bricks.network.social.navigation.ContactNavigator;
import kalecser.sneer.bricks.network.social.navigation.ContactOfContact;
import kalecser.sneer.bricks.snapps.contacts.gui.navigation.NavigateContactsOfContactWindow;
import sneer.bricks.identity.seals.Seal;
import sneer.bricks.identity.seals.contacts.ContactSeals;
import sneer.bricks.network.social.Contact;
import sneer.bricks.network.social.Contacts;
import sneer.bricks.pulp.reactive.collections.CollectionSignals;
import sneer.bricks.pulp.reactive.collections.ListRegister;
import sneer.bricks.skin.widgets.reactive.ListWidget;
import sneer.bricks.skin.widgets.reactive.ReactiveWidgetFactory;
import sneer.bricks.snapps.contacts.actions.ContactAction;
import sneer.bricks.snapps.contacts.actions.ContactActionManager;
import sneer.bricks.snapps.contacts.gui.ContactsGui;
import basis.lang.Consumer;
import basis.lang.exceptions.Refusal;

	public class NavigateContactsOfContactWindowImpl extends JFrame implements NavigateContactsOfContactWindow {
	private JLabel _title = new JLabel("? Friends");
	private boolean wasInit;
	private ListRegister<ContactOfContact> _contactsOfContact;

	{
		addNavigateFriendMenu();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		add (_title, BorderLayout.NORTH);
		_contactsOfContact = my(CollectionSignals.class).newListRegister();
		ListWidget<ContactOfContact> contactsList = my(ReactiveWidgetFactory.class).newList(_contactsOfContact.output());
		JComponent listComponent = contactsList.getComponent();
		listComponent.setPreferredSize(new Dimension(100, 400));
		add (listComponent, BorderLayout.CENTER);
		JButton meToo = new JButton("Me Too");
		addContactUponClick(contactsList, meToo);
		add (meToo, BorderLayout.SOUTH);
		pack();
	}
	
	private void addContactUponClick(final ListWidget<ContactOfContact> contactsList,
			JButton meToo) {
		meToo.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent arg0) {
			addContact(contactsList.selectedElement().currentValue());
		}});
	}
	
	private void addNavigateFriendMenu() {
		my(ContactActionManager.class).addContactAction(new ContactAction(){
			@Override public boolean isEnabled() { return true; }
			@Override public boolean isVisible() { return true; }
			@Override public String caption() { return "Navigate Friends...";}
			@Override public void run() {
				open();
			}
			
			@Override public int positionInMenu() { return 200; }
		});
	}
	
	private void addContact(ContactOfContact contactToAdd) {
		my(Contacts.class).produceContact(contactToAdd.nick);
		try {
			my(ContactSeals.class).put(contactToAdd.nick, contactToAdd.contactSeal);
		} catch (Refusal e) {
			throw new basis.lang.exceptions.NotImplementedYet(e); // Fix Handle this exception.
		}
	}
	
	private void updateTitle() {
		_title.setText(selectedContact().nickname().currentValue() + " Friends");
	}

	private void open() {
		if (!wasInit) {
			init();
			wasInit = true;
		}
		updateTitle();
		requestContactsOfContact();
		setVisible(true);
	}

	private void requestContactsOfContact() {
		clearContactsOfContact();
		Consumer<ContactOfContact> consumer = new Consumer<ContactOfContact>() { @Override public void consume(ContactOfContact value) {
			//if(!_contactsOfContact.output().currentElements().contains(value))
				_contactsOfContact.add(value);
		}};
		my(ContactNavigator.class).searchContactsOf(selectedSeal(), consumer );		
	}

	private Seal selectedSeal() {
		return my(ContactSeals.class).sealGiven(selectedContact()).currentValue();
	}

	private Contact selectedContact() {
		Contact selectedContact = my(ContactsGui.class).selectedContact().currentValue();
		return selectedContact;
	}
	private void clearContactsOfContact() {
		for (ContactOfContact each : _contactsOfContact.output()){
			_contactsOfContact.remove(each);
		}
	};
}