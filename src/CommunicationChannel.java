import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class that implements the channel used by wizards and miners to communicate.
 */
public class CommunicationChannel {
	private BlockingQueue<Message> messagesFromWizard;
	private BlockingQueue<Message> messagesFromMiner;
	private final ReentrantLock lock = new ReentrantLock();
	final static Semaphore minerSemaphore = new Semaphore(1);

	private final Message endMessage = new Message(-1, "END");

	/**
	 * Creates a {@code CommunicationChannel} object.
	 */
	public CommunicationChannel() {
		messagesFromWizard = new LinkedBlockingDeque<>();
		messagesFromMiner = new LinkedBlockingDeque<>();
	}

	/**
	 * Puts a message on the miner channel (i.e., where miners write to and wizards
	 * read from).
	 * 
	 * @param message
	 *            message to be put on the channel
	 */
	public void putMessageMinerChannel(final Message message) {
		try {
			messagesFromMiner.put(message);
		} catch (Exception e) {}
	}

	/**
	 * Gets a message from the miner channel (i.e., where miners write to and
	 * wizards read from).
	 * 
	 * @return message from the miner channel
	 */
	public Message getMessageMinerChannel() {
		Message message = null;
		try {
			message = messagesFromMiner.take();
		} catch (Exception e) {}
		return message;
	}

	/**
	 * Puts a message on the wizard channel (i.e., where wizards write to and miners
	 * read from).
	 * 
	 * @param message
	 *            message to be put on the channel
	 */
	public void putMessageWizardChannel(final Message message) {
		lock.lock();
		try {
			if (message.getCurrentRoom() == endMessage.getCurrentRoom()
					&& message.getData().equals(endMessage.getData())) {
				int numUnlocks = lock.getHoldCount();
				for (int i = 0; i < numUnlocks; ++i)
					lock.unlock();
				return;
			}
			messagesFromWizard.put(message);
		} catch (Exception e) {}
	}

	/**
	 * Gets a message from the wizard channel (i.e., where wizards write to and
	 * miners read from).
	 * 
	 * @return message from the miner channel
	 */
	public Message getMessageWizardChannel() {
		Message message = null;
		try {
			message = messagesFromWizard.take();
		} catch (Exception e) {}
		return message;
	}
}
