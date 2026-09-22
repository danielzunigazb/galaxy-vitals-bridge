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

const Node: React.FC<{
  name: string;
  icon: string;
  label: string;
  delay: number;
  accent: string;
}> = ({ name, icon, label, delay, accent }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <Interactive.Div
      name={name}
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: 14,
        opacity: interpolate(frame, [delay, delay + 0.4 * fps], [0, 1], {
          extrapolateLeft: "clamp",
          extrapolateRight: "clamp",
          easing: Easing.bezier(0.16, 1, 0.3, 1),
        }),
        translate: interpolate(frame, [delay, delay + 0.4 * fps], ["0px 24px", "0px 0px"], {
          extrapolateLeft: "clamp",
          extrapolateRight: "clamp",
          easing: Easing.bezier(0.16, 1, 0.3, 1),
        }),
      }}
    >
      <div
        style={{
          width: 128,
          height: 128,
          borderRadius: 24,
          backgroundColor: "#15131c",
          border: `4px solid ${accent}`,
          boxShadow: `6px 6px 0 ${accent}`,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: 64,
        }}
      >
        {icon}
      </div>
      <div style={{ fontFamily: monoFont, fontWeight: 700, fontSize: 22, color: "#f7f4ff", textAlign: "center" }}>
        {label}
      </div>
    </Interactive.Div>
  );
};

export const Bridge: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const arrowOpacity = (delay: number) =>
    interpolate(frame, [delay, delay + 0.3 * fps], [0, 1], {
      extrapolateLeft: "clamp",
      extrapolateRight: "clamp",
    });

  return (
    <AbsoluteFill
      name="Bridge background"
      style={{
        backgroundColor: "#0a0a0f",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        fontFamily: sansFont,
        gap: 64,
      }}
    >
      <Interactive.Div
        name="Bridge title"
        style={{
          fontFamily: displayFont,
          fontSize: 72,
          color: "#f7f4ff",
          opacity: interpolate(frame, [0, 0.4 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        EL <span style={{ color: "oklch(72% 0.19 255)" }}>BRIDGE</span>
      </Interactive.Div>

      <div style={{ display: "flex", alignItems: "center", gap: 28 }}>
        <Node name="Samsung Health node" icon="⌚" label="SAMSUNG HEALTH" delay={0.5 * fps} accent="oklch(72% 0.24 350)" />
        <Node name="Spotify node" icon="🎧" label="SPOTIFY" delay={0.75 * fps} accent="oklch(76% 0.21 150)" />
        <Node name="Location node" icon="📍" label="UBICACIÓN" delay={1 * fps} accent="oklch(88% 0.17 95)" />

        <div style={{ fontSize: 56, color: "#7a7591", opacity: arrowOpacity(1.3 * fps) }}>→</div>

        <Node name="Phone node" icon="📱" label="WORKMANAGER" delay={1.5 * fps} accent="oklch(72% 0.19 255)" />

        <div style={{ fontSize: 56, color: "#7a7591", opacity: arrowOpacity(1.8 * fps) }}>→</div>

        <Node name="GitHub node" icon="🐙" label="VITALS.JSON" delay={2 * fps} accent="#f7f4ff" />
      </div>

      <Interactive.Div
        name="Bridge caption"
        style={{
          fontFamily: monoFont,
          fontWeight: 600,
          fontSize: 28,
          color: "#b3aec7",
          opacity: interpolate(frame, [2.4 * fps, 2.9 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        cada ~15 min, automático, 100% en tu teléfono
      </Interactive.Div>
    </AbsoluteFill>
  );
};
