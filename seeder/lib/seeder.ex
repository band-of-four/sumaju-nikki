defmodule Seeder do
  alias Seeder.{Repo, Lesson, Room, Creature, Spell}

  def run do
    seq_reset_queries = [
      "ALTER SEQUENCE lessons_id_seq RESTART;",
      "ALTER SEQUENCE spells_id_seq RESTART;",
      "ALTER SEQUENCE rooms_number_seq RESTART;",
      "ALTER SEQUENCE creatures_id_seq RESTART;"
    ]

    Repo.delete_all(Room)
    Repo.delete_all(Lesson)
    Repo.delete_all(Creature)
    Repo.delete_all(Spell)

    seq_reset_queries
    |> Enum.map(fn reset_qry -> Ecto.Adapters.SQL.query!(Repo, reset_qry, []) end)

    lessons =
      Lesson.records()
      |> Enum.map(fn {level, lessons} ->
        {level, Enum.map(lessons, &Repo.insert!/1)}
      end)

    Room.records(lessons)
    |> Map.values()
    |> List.flatten()
    |> Enum.each(&Repo.insert!/1)

    Creature.records()
    |> Map.values()
    |> List.flatten()
    |> Enum.each(&Repo.insert!/1)

    Spell.records()
    |> Map.values()
    |> List.flatten()
    |> Enum.each(&Repo.insert!/1)
  end
end
