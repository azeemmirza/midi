/**
 * Created by Azeem on 6/5/2016.
 */
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.io.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.sound.midi.*;

public class MiniMidiSynth extends JFrame {
    public MiniMidiSynth() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("Mini Midi Synthesizer");

        Box box = Box.createVerticalBox();
        getContentPane().add(box);

        //Access the synthesizer
        try {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
        } catch (MidiUnavailableException e) {
            JOptionPane.showMessageDialog(null, "No synthesizer available - terminating ...", "MIDI ERROR", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        MidiChannel[] channels = synthesizer.getChannels();
        for (int i = 0; i < channels.length; i++) {
            if (channels[i] != null) {
                channel = channels[i];
                break;
            }
        }

        instruments = synthesizer.getAvailableInstruments();
        if (instruments.length == 0) {
            JOptionPane.showMessageDialog(null, "No instruments available - terminating ...", "MIDI ERROR", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        //Create instrument list in combo box
        instrumentChoice = new JComboBox();
        for (int i = 0; i < Math.min(128, instruments.length); i++)
            instrumentChoice.addItem(instruments[i]);

        //Select the first instrument for channel
        channel.programChange(instruments[0].getPatch().getProgram());
        instrumentChoice.setSelectedIndex(0); //Select the chosen

        //Add Listener for new instrument selected
        instrumentChoice.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Patch patch = ((Instrument) instrumentChoice.getSelectedItem()).getPatch();
                channel.programChange(patch.getBank(), patch.getProgram());
            }
        });

        JPanel instrumentPane = new JPanel(new FlowLayout()); //Panel for the instrument
        instrumentPane.add(instrumentChoice);
        box.add(Box.createVerticalStrut(10));
        box.add(instrumentPane);
        box.add(Box.createVerticalStrut(10));

        //Keyboard Creation
        JPanel kbPane = new JPanel(new BorderLayout());
        kbPane.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        kbPane.add(new Keyboard());
        box.add(kbPane);

        pack();
        setVisible(true);
    }

    public static void main (String [] args){
        MiniMidiSynth synth = new MiniMidiSynth();
    }

    class Keyboard extends JPanel
    {

        public Keyboard(){
          setLayout(new BorderLayout());
            setPreferredSize(new Dimension(7*OCTAVES*Key.width,Key.height+1));
            int firstKeyNum = 60- 6 * OCTAVES;
            int whiteIDs[] = {0,2,4,5,7,9,11};
            int blackIDs [] = {0,1,3,0,6,10};
            int position = 0;

            int whiteKeyIndex = 0;
            int blackKeyIndex = 0;
            for (int i=0; i < OCTAVES; i++){
                int KeyNum = i *12 + firstKeyNum;
                for (int j = 0; j<7; j++, position += Key.width)
                {
                    whiteKeys[whiteKeyIndex++] = new Key (position, 0, KeyNum + whiteIDs[j], Color.white);

                    if (j==0 || j==3) continue;
                    else
                        blackKeys[blackKeyIndex++] = new Key(position-Key.width/4, 0, KeyNum+blackIDs[j], Color.black);
                }
            }
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    pressedKey = getKey(e.getPoint());
                    if (pressedKey == null) return;

                    pressedKey.press();
                    repaint();
                }

                public void mouseReleased (MouseEvent e){
                    if (pressedKey != null){
                        pressedKey.release();
                        repaint();
                    }
                }
            });
        }

        //Find the key at a point
        public Key getKey(Point point){
            for (int i = 0; i< blackKeys.length; i++)
                if (blackKeys[i].contains(point))
                    return blackKeys[i];

            for (int i = 0; i<whiteKeys.length; i++)
                if(whiteKeys[i].contains(point))
                    return whiteKeys[i];
            return null;
        }

        public void paint (Graphics g){

            Graphics2D g2d = (Graphics2D) g;
            for (int i = 0; i < whiteKeys.length; i++)
                whiteKeys[i].draw(g2d);

            for (int i = 0; i < blackKeys.length; i++)
                blackKeys[i].draw(g2d);

        }
        final int OCTAVES = 4;
        Key[] whiteKeys = new Key[7*OCTAVES];
        Key[] blackKeys = new Key[5*OCTAVES];
        Key pressedKey;
        //Inner class definning Key

        class Key extends Rectangle
        {
            public Key (int x, int y, int num, Color color){
                super(x,y, color.equals(Color.white)?width:width/2, color.equals(Color.white)?height:height/2);

                this.color = color;
                noteNumber = num;
            }

            public void press(){
                keydown = true;
                channel.noteOn(noteNumber, velocity);
            }

            //Release the key

            public void release(){
                keydown = false;
                channel.noteOff(noteNumber, velocity/2);
            }

            //Draw the key
            public void draw (Graphics2D g2d){
                g2d.setColor(keydown ? Color.blue : color);
                g2d.fill(this);
                g2d.setColor(Color.BLACK);
                g2d.draw(this);
            }

            final static int width = 20;
            final static int height = 100;
            private boolean keydown = false;
            private Color color;
            int noteNumber;
        }
    }

    private Synthesizer synthesizer;
    private MidiChannel channel;
    private Instrument instruments[];
    private JComboBox instrumentChoice;
    private static int velocity = 70;
}
