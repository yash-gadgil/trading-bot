from pathlib import Path

import torch as th
from stable_baselines3 import PPO

MODEL_STEM = "ppo_trading_agent_multi_new"

HERE = Path(__file__).resolve().parent
PROJECT_ROOT = HERE.parents[7]
OUT_DIR = PROJECT_ROOT / "src" / "main" / "resources" / "models"
OUT_DIR.mkdir(parents=True, exist_ok=True)

class OnnxablePolicy(th.nn.Module):

    def __init__(self, policy):
        super().__init__()
        self.policy = policy

    def forward(self, observation: th.Tensor) -> th.Tensor:
        features = self.policy.extract_features(observation)
        if isinstance(features, tuple):
            features = features[0]
        latent_pi, _ = self.policy.mlp_extractor(features)
        return self.policy.action_net(latent_pi)

def main() -> None:
    model = PPO.load(str(HERE / MODEL_STEM), device="cpu")
    onnx_policy = OnnxablePolicy(model.policy)
    onnx_policy.eval()

    obs_shape = model.observation_space.shape
    dummy = th.randn(1, *obs_shape, dtype=th.float32)

    out_path = OUT_DIR / f"{MODEL_STEM}.onnx"
    th.onnx.export(
        onnx_policy,
        dummy,
        str(out_path),
        opset_version=17,
        input_names=["observation"],
        output_names=["logits"],
        dynamic_axes={
            "observation": {0: "batch"},
            "logits": {0: "batch"},
        },
        external_data=False,
    )
    print(f"Exported ONNX model to: {out_path}")
    print(f"Input shape: [batch, {obs_shape[0]}]  |  Output shape: [batch, 5]")

if __name__ == "__main__":
    main()
