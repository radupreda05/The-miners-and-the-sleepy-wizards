import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

/**
 * Class for a miner.
 */
public class Miner extends Thread {
    private Integer hashCount;
    private Set<Integer> solvedHash;
    private CommunicationChannel communicationChannel;

    /**
     * Creates a {@code Miner} object.
     *
     * @param hashCount number of times that a miner repeats the hash operation when
     *                  solving a puzzle.
     * @param solved    set containing the IDs of the solved rooms
     * @param channel   communication channel between the miners and the wizards
     */
    public Miner(Integer hashCount, Set<Integer> solved, CommunicationChannel channel) {
        this.hashCount = hashCount;
        this.solvedHash = solved;
        this.communicationChannel = channel;
    }

    /**
     * At each iteration get 2 messages from the communication channel.
     * To assure that each miner gets a node and it's parent i used a semaphore.
     * A miner tries to acquire the semaphore, reads 2 messages and then it
     * releases it.
     */
    @Override
    public void run() {
        while (true) {
            try {
                CommunicationChannel.minerSemaphore.acquire();
            } catch (Exception e) {}

            Message currentRoom = communicationChannel.getMessageWizardChannel();
            Message adjacentRoom = communicationChannel.getMessageWizardChannel();
            CommunicationChannel.minerSemaphore.release();

            if (solvedHash.contains(adjacentRoom.getCurrentRoom()))
                continue;

            String hashed = encryptMultipleTimes(adjacentRoom.getData(), hashCount);
            solvedHash.add(adjacentRoom.getCurrentRoom());
            Message messageToWizard = new Message(currentRoom.getCurrentRoom(),
                    adjacentRoom.getCurrentRoom(), hashed);
            communicationChannel.putMessageMinerChannel(messageToWizard);
        }
    }

    private static String encryptMultipleTimes(String input, Integer count) {
        String hashed = input;
        for (int i = 0; i < count; ++i) {
            hashed = encryptThisString(hashed);
        }

        return hashed;
    }

    private static String encryptThisString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));

            // convert to string
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xff & messageDigest[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
