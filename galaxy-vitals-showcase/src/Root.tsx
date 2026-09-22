import "./index.css";
import { Composition, Folder } from "remotion";
import { GalaxyVitalsShowcase } from "./GalaxyVitalsShowcase";
import { Intro } from "./scenes/Intro";
import { Bridge } from "./scenes/Bridge";
import { StatusPage } from "./scenes/StatusPage";
import { Widget } from "./scenes/Widget";
import { Outro } from "./scenes/Outro";

export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Folder name="GalaxyVitalsShowcase-Scenes">
        <Composition id="Intro" component={Intro} durationInFrames={90} fps={30} width={1920} height={1080} />
        <Composition id="Bridge" component={Bridge} durationInFrames={150} fps={30} width={1920} height={1080} />
        <Composition id="StatusPage" component={StatusPage} durationInFrames={180} fps={30} width={1920} height={1080} />
        <Composition id="Widget" component={Widget} durationInFrames={120} fps={30} width={1920} height={1080} />
        <Composition id="Outro" component={Outro} durationInFrames={100} fps={30} width={1920} height={1080} />
      </Folder>
      <Composition
        id="GalaxyVitalsShowcase"
        component={GalaxyVitalsShowcase}
        durationInFrames={560}
        fps={30}
        width={1920}
        height={1080}
      />
    </>
  );
};
