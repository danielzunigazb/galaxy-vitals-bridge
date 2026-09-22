import { TransitionSeries, linearTiming } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { Intro } from "./scenes/Intro";
import { Bridge } from "./scenes/Bridge";
import { StatusPage } from "./scenes/StatusPage";
import { Widget } from "./scenes/Widget";
import { Outro } from "./scenes/Outro";

export const GalaxyVitalsShowcase: React.FC = () => {
  return (
    <TransitionSeries>
      <TransitionSeries.Sequence durationInFrames={90} name="Intro">
        <Intro />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 20 })} />

      <TransitionSeries.Sequence durationInFrames={150} name="Bridge">
        <Bridge />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 20 })} />

      <TransitionSeries.Sequence durationInFrames={180} name="StatusPage">
        <StatusPage />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 20 })} />

      <TransitionSeries.Sequence durationInFrames={120} name="Widget">
        <Widget />
      </TransitionSeries.Sequence>
      <TransitionSeries.Transition presentation={fade()} timing={linearTiming({ durationInFrames: 20 })} />

      <TransitionSeries.Sequence durationInFrames={100} name="Outro">
        <Outro />
      </TransitionSeries.Sequence>
    </TransitionSeries>
  );
};
