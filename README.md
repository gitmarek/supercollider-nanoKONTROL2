# supercollider-nanoKONTROL2
SuperCollider MIDI handlers for KORG nanoKONTROL2.

The purpose of this library is to make possible for the MIDI device to easily
control instruments with multiple parameters, and not to serve as a mixing
board.  Hence somewhat unorthodox approach to storing MIDI data and to what
different parts of the device mean.  In the future, it will be possible to
switch between *device* and *mixer* mode, the latter being more traditional.


## Instructions how to operate the device

It is not a bad idea to set the state of the device to factory defaults
before using it with this library.  This way you will be sure all the MIDI
data coming from the device have standard values.  In order to do so, disconnect
the device, then press three buttons: Cycle, and two Tracks, and connect it
again while holding the buttons.  You will see the LEDs of the transport buttons
blink.  Do not disconnect the device until the LEDs stop blinking, which usually
lasts about a second.

The meaning of buttons (S, M, R) is rather unusual:

* if S (slow) is pressed, then the adjacent fader or knob reacts to
  changes very slowly around the previous value;
* if M (mute) is pressed, then the adjacent fader or knob does not react
  to changes until the button is released; then it assumes a new value
  in a discontinuous way;
* if R (random) is pressed and adjacent knob of fader is touched at
  the right value, then it assumes a random value from (0,1).

The M button takes precedence over S, which in turn takes precedence over R.

The are a number of scenes, each for independent set of eight knobs and faders.
If a scene changes (by pressing Track buttons) to another one and then
changes back, then the knobs or faders have to be set at the previous value
for the first scene, so they do not jump discontinuously.  If this value is set,
so that a knob or fader is active, a LED turns on: adjacent S button LED for
knobs and M button LED for faders.


## Installation
Put a copy of `nanoKONTROLL2.sc` in your SuperCollider extensions directory,
recompile class library (Ctrl+Shift+L).


## Usage within SuperCollider
First, connect the device by e.g. `MIDIIn.connectAll;`, find the uid of the
port the controller is connected to and specify it as a `srcID` argument.
If nanoKONTROL2 is connected to one of the out ports by e.g.
`MIDIOut.connect(outport, device)`, where `device` is its position in the
`MIDIClient.destinations` list, then you can specify the outport number as well
in order to enable LED functionality.  The class will try to put nanoKONTROL2
into the external LED mode.  If you want to return to the internal led mode
(I have no idea, what for), please use KORG Kontrol Editor, or set the factory
defaults for the device.

The code reads controller MIDI data and maps them linearly to buses
at control rate.  The data from faders is mapped to the interval `[0,1]`;
whereas the data from knobs to `[-1,1]`.

Create a new instance of the class: `n = NanoKONTROL2(server)`.
Access knobs and faders by `n.faders` and `n.knobs`, two-dimensional arrays,
for which the first index denotes the scene number.  You can treat each element
of the arrays as a UGen at control rate by adding `.kr` message.  Use it in
your SynthDefs.  For instance, `n.knobs[2][7].kr` gives you current value of
the 8th knob in the 3rd scene.

For each button key: `backwards`, `forwards`, `stop`, `play` and `rec`,
the user can specify a function `key_action(scene)`, as well as the global one:
`transport_action(scene)`.  Each time a transport button is pressed,
the `transport_action()` function is evaluated first, and then the appropriate
`key_action()` function with the current scene number passed as an argument.
The transport buttons are instances of the class `NanoKONTROL2Button` and
can be accessed as `key_button`.

**Keep in mind that as long as the software version is less then 1.0,**
**backward compatibility could be broken from one 0.x version to the next.**


### Example

```SuperCollider

MIDIClient.init;
// MIDIFunc.trace;
MIDIIn.connectAll;

~nK2srcID = MIDIIn.findPort("nanoKONTROL2-nanoKONTROL2 MIDI 1","nanoKONTROL2-nanoKONTROL2 MIDI 1").uid;
~nK2MIDIOut = MIDIOut.newByName("nanoKONTROL2-nanoKONTROL2 MIDI 1","nanoKONTROL2-nanoKONTROL2 MIDI 1");
~nk2OutPort = 0;

MIDIOut.connect(~nk2OutPort,~nK2MIDIOut.port);

n = NanoKONTROL2(s, srcID: ~nK2scrID, outport: ~nk2OutPort);

(
Ndef(
     \NK2_test, { | freq = 440 |
         Out.ar(0, Pan2.ar(
            SinOsc.ar( n.knobs[0][1].kr*400 + 800,
                 mul: n.faders[0][3].kr.linexp(0,1,0.001,1)
             ),
             pos: n.knobs[2][2].kr)
         );
     }
);
)
Ndef(\NK2_test).pause;
Ndef(\NK2_test).resume;
Ndef(\NK2_test).free;

n.free;
s.quit;
```

## Authors
Marek Miller, <marek.l.miller@gmail.com>


## License
This software licensed is under the MIT license.  Feel free to use it however
you like!  For more information, see [LICENSE](./LICENSE).

