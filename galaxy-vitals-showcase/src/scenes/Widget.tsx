import {
  AbsoluteFill,
  Easing,
  Interactive,
  interpolate,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { loadFont as loadDisplay } from "@remotion/google-fonts/Anton";
import { loadFont as loadMono } from "@remotion/google-fonts/JetBrainsMono";
import { loadFont as loadSans } from "@remotion/google-fonts/SpaceGrotesk";

const { fontFamily: displayFont } = loadDisplay("normal", { weights: ["400"], subsets: ["latin"] });
const { fontFamily: monoFont } = loadMono("normal", { weights: ["600", "700"], subsets: ["latin"] });
const { fontFamily: sansFont } = loadSans("normal", { weights: ["500", "700"], subsets: ["latin"] });

export const Widget: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  // Button press pulse around the 2.2s mark.
  const pressCenter = 2.2 * fps;
  const buttonScale = interpolate(
    frame,
    [pressCenter - 4, pressCenter, pressCenter + 8],
    [1, 0.94, 1],
    {
      extrapolateLeft: "clamp",
      extrapolateRight: "clamp",
      easing: Easing.spring({ damping: 200 }),
      output: "perceptual-scale",
    },
  );

  return (
    <AbsoluteFill
      name="Widget background"
      style={{
        backgroundColor: "#0a0a0f",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        fontFamily: sansFont,
        gap: 48,
      }}
    >
      <Interactive.Div
        name="Widget title"
        style={{
          fontFamily: displayFont,
          fontSize: 72,
          color: "#f7f4ff",
          textAlign: "center",
          opacity: interpolate(frame, [0, 0.4 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        SIN ABRIR LA <span style={{ color: "oklch(88% 0.17 95)" }}>APP</span>
      </Interactive.Div>

      <Interactive.Div
        name="Widget card"
        style={{
          width: 460,
          backgroundColor: "#1c1926",
          border: "4px solid #f7f4ff",
          boxShadow: "8px 8px 0 oklch(72% 0.24 350)",
          padding: 28,
        
          opacity: interpolate(frame, [0.4 * fps, 1 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1)
          }),
        
          scale: interpolate(frame, [0.4 * fps, 1 * fps], [0.9, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
        
            easing: Easing.spring({
              damping: 200
            }),
        
            output: "perceptual-scale"
          })
        }}
      >
        <div style={{ fontFamily: monoFont, fontWeight: 700, fontSize: 18, color: "#7a7591", letterSpacing: 1 }}>
          GALAXY VITALS
        </div>
        <div style={{ fontFamily: monoFont, fontWeight: 700, fontSize: 40, color: "#f7f4ff", marginTop: 10 }}>
          ♥ 78 bpm
        </div>
        <div style={{ fontFamily: monoFont, fontSize: 24, color: "#b3aec7", marginTop: 6 }}>👟 6958 pasos</div>
        <div style={{ fontFamily: monoFont, fontSize: 24, color: "#b3aec7" }}>📍 en casa</div>
        <div style={{ fontFamily: monoFont, fontSize: 16, color: "#7a7591", marginTop: 10 }}>sync 19:44</div>
        <div
          style={{
            marginTop: 16,
            padding: "14px 0",
            textAlign: "center",
            backgroundColor: "oklch(72% 0.24 350)",
            color: "#0a0a0f",
            fontFamily: monoFont,
            fontWeight: 700,
            fontSize: 20,
            scale: buttonScale,
          }}
        >
          SYNC AHORA
        </div>
      </Interactive.Div>

      <Interactive.Div
        name="Widget caption"
        style={{
          fontFamily: monoFont,
          fontWeight: 600,
          fontSize: 26,
          color: "#7a7591",
          opacity: interpolate(frame, [2.8 * fps, 3.3 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        un toque en la pantalla de inicio, y listo
      </Interactive.Div>
    </AbsoluteFill>
  );
};
