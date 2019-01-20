defmodule Seeder.Owl do
  use Ecto.Schema
  alias Seeder.Owl

  @data YamlElixir.read_from_file!("owls.yaml", atoms: true)

  @primary_key{:impl, :string, []}
  schema "owls" do
    field :display_name, :string
    field :description, :string
    field :applicable_stages, {:array, Seeder.Note.StudentStage}
    field :stages_active, :integer
    field :is_immediate, :boolean
    field :occurrence, :float
  end

  def records do
    @data
    |> Enum.map(fn %{impl: impl, display_name: d_name, description: desc,
      applicable_stages: app_stgs, stages_active: stg_act, is_immediate: imm,
      occurrence: occ} ->
      %Owl{impl: impl, display_name: d_name, description: desc,       
        applicable_stages: app_stgs, stages_active: stg_act, is_immediate: imm,
        occurrence: occ}
    end)
  end
end
