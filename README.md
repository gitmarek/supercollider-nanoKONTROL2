# supercollider-nanoKONTROL2
SuperCollider MIDI handlers for KORG nanoKONTROL2.

The purpose of this library is to make possible for the MIDI device to easily
control instruments with multiple parameters, and not to serve as a mixing
board.  Hence somewhat unorthodox approach to storing MIDI data and to what
different parts of the device mean.  In the future, it will be possible to
switch between *device* and *mixer* mode, the latter being more traditional.


## Instructions how to operate the device

If a scene changes (by pressing Track buttons) to another one and then
changes back, then the knobs or faders have to be set at the previous value
for that scene, so they do not jump discontinuously.

The meaning of buttons (S, M, R) is rather unusual:

1. if S (slow) is pressed, then the adjacent fader or knob reacts to
   changes very slowly around the previous value;
2. if M (mute) is pressed, then the adjacent fader or knob does not react
   to changes until the button is released; then it assumes a new value
   in a discontinuous way;
3. if R (random) is pressed and adjacent knob of fader is touched at
   the right value, then it assumes a random value from (0,1).

The M button takes precedence over S, which in turn takes precedence over R.


## Installation
Put a copy of nanoKONTROLL2.sc in your SuperCollider extensions directory,
recompile class library (Ctrl+Shift+L).


## Usage within SuperCollider
The code reads controller MIDI data and maps them linearly to buses
at control rate.  The data from faders is mapped to the interval `[0,1]`;
whereas the data from knobs to `[-1,1]`.

Create a new instance of the class: `n = NanoKONTROL2(server)`.
Access knobs and faders by n.faders and n.knobs, two-dimensional arrays, for
which the first index denotes the scene number. You can treat each element
of the arrays as a UGen at control rate by adding .kr message.  Use it in
your SynthDefs. For instance, `n.knobs[2][7].kr` gives you current value of
the 8th knob in the 3rd scene.

Keep in mind that as long as the software version is less then 1.0,
backward compatibility could be broken from one 0.x version to the next.


### Example (assuming you are happy with all default values):
```SuperCollider

(
o = Server.internal.options;
Server.default = s = Server.internal.reboot;
)

MIDIClient.init;
MIDIIn.connectAll;
MIDIClient.sources;
// suppose your device is connected to port 3:
~nK2srcID = MIDIClient.sources[3].uid; // this is your srcID number.
MIDIFunc.trace;

n = NanoKONTROL2(s, srcID: ~nK2srcID);

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
This software licensed is under the MIT license. Feel free to use it however
you like!  For more information, see [LICENSE](./LICENSE).

