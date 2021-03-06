defmodule Orion.Nikki do

  @token "trust-me-im-orion"

  def get_unapproved_creature() do
    case HTTPoison.get("localhost:9090/bot/creatures/unapproved", [{"Token", @token}]) do
      {:ok, %HTTPoison.Response{status_code: 200, body: body}} ->
        Poison.Parser.parse!(body, %{keys: :atoms}) 
      {:ok, %HTTPoison.Response{status_code: 404}} ->
        nil
      e ->
        IO.inspect e
        nil
    end
  end

  def get_unapproved_note do
    case HTTPoison.get("localhost:9090/bot/notes/unapproved", [{"Token", @token}]) do
      {:ok, %HTTPoison.Response{status_code: 200, body: body}} ->
        Poison.Parser.parse!(body, %{keys: :atoms}) 
      {:ok, %HTTPoison.Response{status_code: 404}} ->
        nil
      e ->
        IO.inspect e
        nil
    end
  end

  def post_approved_creature(data) do
    HTTPoison.post "localhost:9090/bot/creatures/approve", Poison.encode!(data), 
      [{"Content-Type", "application/json"}, {"Token", @token}]
  end

  def post_approved_note(data) do
    HTTPoison.post "localhost:9090/bot/notes/approve", Poison.encode!(data), 
      [{"Content-Type", "application/json"}, {"Token", @token}]
  end
end
